# ARCTIC-Shard
Shard is the plugin engine that connects ARCTIC to a hypervisor or cloud provider. It handles plugin loading, configuration management, client creation, and the execution of range build tasks.

## Plugin System Overview
Shard uses a runtime plugin system. Each provider connector is packaged as a JAR and placed in the `providers/` folder alongside the running application. On startup, `ShardManager` scans that folder, reads each JAR's descriptor, and registers the plugin automatically. No code changes to ARCTIC are needed to add a new provider.

```
providers/
  openstack-shard.jar
  my-new-provider.jar
```

---

## Building a New Provider

Creating a new provider connector requires understanding the target hypervisor's Java library or API before starting. For the purposes of this README, all examples use OpenStack via the OpenStack4J library. Documentation can be found at: https://openstack4j.github.io/

### Step 1: The shard.yml Descriptor
Every plugin JAR must include a `shard.yml` file at the root of the JAR. This file tells Shard what class to load, what version the plugin is, and what configuration keys the plugin requires. The configuration keys defined here are automatically registered in the database and surfaced to the user through the ARCTIC UI.

```yaml
class: com.example.MyProviderShard
version: 1.0
config_settings:
  endpoint:
    type: "url"
    required: true
  username:
    type: "string"
    required: true
  password:
    type: "password"
    required: true
  domain:
    type: "string"
    required: false
```

**Supported types:** `string`, `password`, `url`

Required fields will be validated before a session is created. If a required field is missing, the build will be rejected before your plugin is called.

---

### Step 2: Defining the Provider Class
Create a class that extends `ShardProviderTmpl` and type it with the client object your hypervisor library provides.

**Template:**
```java
public class ProviderExampleShard extends ShardProviderTmpl<ClientClass> {

    @Override
    public String getDomain() {
        return "provider"; // Must be unique. Must match what users select in the UI.
    }

    // Provider code here
}
```

**Practical Example:**
```java
public class OpenStackShard extends ShardProviderTmpl<OSClientV3> {

    @Override
    public String getDomain() {
        return "openstack";
    }

    // Provider code here
}
```

`getDomain()` returns the unique identifier for your plugin. It is used as the key in the plugin registry and must match the domain string users reference when creating a provider profile.

---

### Step 3: Plugin Lifecycle Methods
Two optional lifecycle hooks are available. Override them if your plugin needs setup or teardown logic.

```java
@Override
public void pluginEnabled() {
    // Called once when the plugin is successfully loaded at startup.
    // Use this to register UI field creators (see UI Fields section below).
    System.out.println("[openstack] Plugin loaded");
}

@Override
public void pluginDisabled() {
    // Called when the application is shutting down.
    System.out.println("[openstack] Plugin unloaded");
}
```

---

### Step 4: Creating the Client
`createClient` is called each time a new session is needed. It receives a `ShardProfileSettingsReference` containing the user's decrypted configuration values for this profile. Return your connected client object, or call `failWithMessage()` and return `null` if a required setting is missing or the connection fails.

```java
@Override
public OSClientV3 createClient(ShardProfileSettingsReference config) {
    String endpoint  = config.getConfiguration("endpoint");
    String username  = config.getConfiguration("username");
    String password  = config.getConfiguration("password");
    String projectId = config.getConfiguration("projectId");

    if (endpoint == null || username == null || password == null || projectId == null) {
        failWithMessage("Required configuration missing. Check provider settings.");
        return null;
    }

    String domain = config.getConfigurationOrDefault("domain", "Default");

    return OSFactory.builderV3()
            .endpoint(endpoint)
            .credentials(username, password, Identifier.byName(domain))
            .scopeToProject(Identifier.byId(projectId))
            .authenticate();
}
```

**Configuration methods on `ShardProfileSettingsReference`:**

| Method | Behavior |
|--------|----------|
| `getConfiguration(key)` | Returns the value or `null` if not set |
| `getConfigurationOrDefault(key, default)` | Returns the value or the default if not set or blank |
| `hasConfiguration(key)` | Returns true if the key exists |

**Error handling:** Use `failWithMessage(String)` instead of throwing exceptions or calling `System.exit()`. This marks the plugin state as errored and logs the message cleanly.

```java
failWithMessage("Could not authenticate with the provided credentials.");
return null;
```

---

### Step 5: Arctic Service Objects (SOs)
ARCTIC passes hypervisor-neutral service objects into your `build*` methods. These carry the fields needed to create each resource. Read what you need — some fields may not apply to your provider.

| SO Class | Key Fields |
|----------|-----------|
| `ArcticHostSO` | `name`, `ip`, `osType`, `rangeId`, `networks` (Set), `volumes` (Set), `extraVariables` (Map) |
| `ArcticNetworkSO` | `name`, `ipCidr`, `ipGateway`, `ipRangeStart`, `ipRangeEnd`, `rangeId` |
| `ArcticVolumeSO` | `name`, `size`, `imageId`, `bootable`, `description`, `rangeId` |
| `ArcticRouterSO` | `name`, `connectedNetworkNames` (Set), `rangeId` |
| `ArcticSecurityGroupSO` | `name`, `description`, `rangeId` |
| `ArcticSecurityGroupRuleSO` | `name`, `direction`, `protocol`, `startPortRange`, `endPortRange`, `eth`, `secGroup`, `rangeId` |

Hypervisor-specific values that don't have a dedicated field (e.g. flavor ID, image ID) are stored in `extraVariables`:

```java
String flavorId = ah.getExtraVariables().get("flavorId");
String imageId  = ah.getExtraVariables().get("osId");
```

After a resource is created, call `setProviderId(String)` on the SO to store the hypervisor-assigned ID back on the object.

---

### Step 6: The ArcticTask
`ArcticTask<Client, Resource>` is the unit of async work. Each `build*` method returns one. The task has two responsibilities: `action()` creates the resource and returns it, and `waitMethod(Resource)` blocks until the resource is ready.

**Constructors:**
```java
new ArcticTask<>(int priority, List<ArcticTask<?, ?>> dependencies) { ... }
```

Lower priority number = built first. Tasks with dependencies will block until all dependency tasks complete before running.

**General priority ordering:**

| Priority | Resource Type |
|----------|--------------|
| 1 | Networks |
| 2 | Address Ranges / Subnets |
| 3 | Volumes |
| 10 | Instances / Hosts |
| Last | Security Groups |

**Task maps are on `ShardRunningContext`**, not the provider. Access them via the `context` argument passed into each `build*` method:

```java
context.getNetworkTasks()       // Map<String, ArcticTask<T, ?>>
context.getInstanceTasks()
context.getVolumeTasks()
context.getRouterTasks()
context.getSecurityGroupTasks()
context.getSecurityGroupRuleTasks()
```

Use `getTypedTask()` for a safe cast when pulling a dependency from a task map:
```java
ArcticTask<OSClientV3, Network> netTask = getTypedTask(context.getNetworkTasks(), networkName);
```

---

### Step 7: Implementing the Build Methods
Extend `ShardProviderTmpl` and implement all six build methods. Each receives the running context and an SO.

```java
protected abstract ArcticTask<T, ?> buildHost(ShardRunningContext<T> context, ArcticHostSO ah);
protected abstract ArcticTask<T, ?> buildNetwork(ShardRunningContext<T> context, ArcticNetworkSO an);
protected abstract ArcticTask<T, ?> buildVolume(ShardRunningContext<T> context, ArcticVolumeSO av);
protected abstract ArcticTask<T, ?> buildRouter(ShardRunningContext<T> context, ArcticRouterSO ar);
protected abstract ArcticTask<T, ?> buildSecurityGroup(ShardRunningContext<T> context, ArcticSecurityGroupSO asg);
protected abstract ArcticTask<T, ?> buildSecurityGroupRule(ShardRunningContext<T> context, ArcticSecurityGroupRuleSO asgr);
```

**Full example — `buildHost` from OpenStackShard:**
```java
@SuppressWarnings("unchecked")
@Override
protected ArcticTask<OSClientV3, Server> buildHost(ShardRunningContext<OSClientV3> context, ArcticHostSO ah) {
    List<ArcticTask<OSClientV3, Volume>>  volumes  = new ArrayList<>();
    List<ArcticTask<OSClientV3, Network>> networks = new ArrayList<>();
    List<ArcticTask<OSClientV3, ?>>       depends  = new ArrayList<>();

    ah.getNetworks().forEach(name -> {
        ArcticTask<OSClientV3, Network> task = getTypedTask(context.getNetworkTasks(), name);
        networks.add(task);
        depends.add(task);
    });

    ah.getVolumes().forEach(name -> {
        ArcticTask<OSClientV3, Volume> task = getTypedTask(context.getVolumeTasks(), name);
        volumes.add(task);
        depends.add(task);
    });

    return new ArcticTask<>(10, depends) {

        @Override
        public Server action() {
            String flavorId = ah.getExtraVariables().get("flavorId");

            ServerCreateBuilder scb = Builders.server();
            scb.name(ah.getName());
            scb.flavor(flavorId);

            for (ArcticTask<OSClientV3, Volume> vol : volumes) {
                scb.blockDevice(Builders.blockDeviceMapping()
                        .uuid(vol.getResource().getId())
                        .bootIndex(0)
                        .destinationType(BDMDestType.VOLUME)
                        .sourceType(BDMSourceType.VOLUME)
                        .deleteOnTermination(true)
                        .build());
            }

            List<String> networkIds = new ArrayList<>();
            for (ArcticTask<OSClientV3, Network> net : networks) {
                networkIds.add(net.getResource().getId());
            }
            scb.networks(networkIds);

            // Always create a fresh client from token — OSClientV3 is not thread-safe
            return OSFactory.clientFromToken(context.getClient().getToken())
                    .compute().servers().boot(scb.build());
        }

        @Override
        public void waitMethod(Server s) {
            Waiter<OSClientV3, Server> waiter = OpenStackWaiter.waitForInstanceAvailable();
            try {
                waiter.waitUntilReady(
                    OSFactory.clientFromToken(context.getClient().getToken()),
                    ah.getRangeId(), s, 5000, 10);
            } catch (ResourceTimeoutException | ResourceErrorException e) {
                e.printStackTrace();
            }
        }
    };
}
```

**Thread safety:** Hypervisor clients are often not thread-safe. Always create a fresh client from the original token when inside `action()` or `waitMethod()`. Never share a client instance across threads.

```java
// Correct — fresh client per thread
OSFactory.clientFromToken(context.getClient().getToken())

// Wrong — sharing the session client directly across threads
context.getClient()
```

---

### Step 8: UI Field Registration (Optional)
If your provider can supply dynamic field values (e.g. a list of available flavors or images to populate a dropdown in the UI), register a `ShardProviderUICreation` handler inside `pluginEnabled()`.

```java
@Override
public void pluginEnabled() {
    registerUICreation(new ObtainFlavors());
    registerUICreation(new ObtainImages());
}
```

Each UI creator implements `ShardProviderUICreation<Client, ReturnType>` and annotates its `returnResult` method with `@UIField` to declare which object type and key it belongs to.

---

## Packaging the Plugin
The plugin must be packaged as an executable JAR with all its dependencies included (fat JAR / shaded JAR). The `shard.yml` must be at the root of the JAR, not inside a subdirectory.

Place the finished JAR in the `providers/` folder next to the running ARCTIC application and restart. Shard will load it automatically on the next startup.
