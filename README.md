# ARCTIC-Shard
Shard is the interface that connects Iceberg with the associated Cloud Provider for building the resources that are used with the range.

## Building A New Provider
Creating a new provider connector can be tedious and time-consuming. It is recommended to know the providers Library/API before moving on.

### Defining the Provider
First Step, is to create a class that extends <b>ShardProviderTmpl</b> and type the Client being provider.

<b>Example</b>
```java
public class ProviderExampleShard extends ShardProviderTmpl<ClientClass> {

  public ProviderExampleShard() {
    super("provider");
  }

  // Provider Code Here
}
```
<b>Practical Example</b>
```java
public class OpenStackShard extends ShardProviderTmpl<OSClientV3> {

  public OpenStackShard() {
    super("openstack");
  }

  // Provider Code Here
}
```
In the above example, the OSClientV3 is the Client object imported from OpenStack4J. The Super method with the string argument tells SHardProviderTmpl where to pull your configuration settings for your specific shard. This is currently stored in a file located at <b>./.providers</b>.

The Settings look like:
```
[provider]
option1 = value1
option2 = value2
option3 = value3

[openstack]
option1 = value1
option2 = value2
option3 = value3
```
### Creating the Client
One of the most important classes is defining and creating the Client object that will be used for creation of all range objects. (I.E. Networks, Routers, Hosts, Volumes....)

The code below outlines the abstract method you'll need to create.
```java
@Override
public T createClient();
```

It is your job to understand how to create the client and what variables are needed for the client to work. Below is an example of connecting to OpenStack. Both the Java file and Config file:

<b>OpenStackShard.java</b>
```java
@Override
public OSClientV3 createClient() {
	String endpoint = getProperties().getPropertyValue("endpoint");
	String username = getProperties().getPropertyValue("username");
	String password = getProperties().getPropertyValue("password");
	String projectId = getProperties().getPropertyValue("projectId");
	
	if(endpoint == null || username == null || password == null || projectId == null) {
		System.out.println("Required configuration details do not exists. Please add and re-run.");
		System.exit(1);
		return null;
	}
	
	String domain = getProperties().getPropertyValue("domain");
	
	if(domain == null) {
		domain = "Default";
	}
	
	OSClientV3 mainOSC = OSFactory.builderV3()
			.endpoint(endpoint)
			.credentials(username, password, Identifier.byName(domain))
			.scopeToProject(Identifier.byId(projectId))
			.authenticate();
	
	return mainOSC;
}
```

<b>./.providers</b>
```
[openstack]
endpoint = 12.0.0.1:8006/api3
username = admin
password = password
projectId = {UID of Admin Project}
domain = Default
```

<b>getProperties()</b> is built when extended <b>ShardProviderTmpl</b> and providing your configuration section string in the <b>super()</b> method.

### Arctic Objects
ARCTIC comes pre-built with wrapper classes that will push information to your new provider connector class. The ARCTIC Object MAY have more information in it then you need to use to create the object for the specific provider. Make sure to read through all variables the Wrapper Object has.

Currently available Arctic Wrapper Objects:

```
ArcticHost
ArcticNetwork
ArcticRouter
ArcticSecurityGroup
ArcticVolume
```

### Arctic Task
<b>We will explore creating the ArcticTask in the next section, this is only meant to help understand what the class is.</b>

The ArcticTask is a thread-ready class that will be used to build, and wait, for your provider object to be ready. There are two ways to create an ArcticTask
```java
public ArcticTask(int priority);
public ArcticTask(int priority, List<ArcticTask<T, ?>> depends);
```

When creating an ArcticTask, it is your job to ensure you know what needs to be built first. A lower numbers means it will be created first (0 is created before 1). The IcebergCreator class will automatically sort the ArcticTasks by priority when building. You will not need to worry about sorting.

Some objects require certain objects to be created <b>before</b> the object can be created. For example, an OpenStack instance needs to have the network and volumes made that you want to add to that host. Be sure you understand what objects are required when building the object.

All resources can be obtain from the following methods inside of your class that extends <b>ShardProviderTmpl</b>
```
getNetworkTasks(); // Network Objects
getInstanceTasks(); // Instance/Host Objects
getSecurityGroupTasks(); // Security Group Objects
getVolumeTasks(); // Volume Objects
getRouterTasks(); // Router Objects
```
<b>These will be available automatically, no need to add objects to these lists</b>

Creating a new ArcticTask will require the following methods:
```java
public abstract R action();
public abstract void waitMethod(R resource);
```

<b>action</b> Is actually connecting to the provider to build out the object you need and returning it.
<b>waitMethod(R resource)</b> Is the method that dictates how you need to wait for your object to be ready before moving on. As well as where you put the Timeout and ResourceNotAvailable exeptions.

### Creator Methods
Extending <b>ShardProviderTmpl</b> will require the following methods:
```java
protected abstract ArcticTask<T,?> buildHost(ArcticHost ah);
protected abstract ArcticTask<T,?> buildNetwork(ArcticNetwork an);
protected abstract ArcticTask<T,?> buildSecurityGroup(ArcticSecurityGroup asg);
protected abstract ArcticTask<T,?> buildRouter(ArcticRouter ar);
protected abstract ArcticTask<T,?> buildVolume(ArcticVolume av);
```
These methods will handle how your specific Cloud Provider, with your specific client, will build the actual object. Currently, all objects must be built by THAT specific client. There is no delegating specific objects off to another client.

Example taken from <b>OpenStackShard</b>
```java
@SuppressWarnings("unchecked")
@Override
protected ArcticTask<OSClientV3, Server> buildHost(ArcticHost ah) {
	// Create the lists that will hold the dependencies needed further into the method
	List<ArcticTask<OSClientV3, Volume>> volumes = new ArrayList<>();
	List<ArcticTask<OSClientV3, Network>> networks = new ArrayList<>();
	List<ArcticTask<OSClientV3, ?>> depends = new ArrayList<>();
	
	// Grab all networks and volumes from ArcticHost and add
	// 		them into the lists above
	ah.getNetworks().forEach(e -> {
		networks.add((ArcticTask<OSClientV3, Network>) getNetworkTasks().get(e));
		depends.add(getNetworkTasks().get(e));
	});
	
	ah.getVolumes().forEach(e -> {
		volumes.add((ArcticTask<OSClientV3, Volume>) getVolumeTasks().get(e));
		depends.add(getVolumeTasks().get(e));
	});
	
	// Create the ArcticTask<Client, Resource>
	ArcticTask<OSClientV3, Server> server = new ArcticTask<>(10, depends) {
		
		// Actual action of building the Server following OSClientV3 Library
		public Server action() {
			ServerCreateBuilder scb = Builders.server();
			scb.name(ah.getName());
			scb.flavor(ah.getFlavor());
			for(ArcticTask<OSClientV3, Volume> vol : volumes) {
				scb.blockDevice(Builders.blockDeviceMapping()
						.uuid(vol.getResource().getId())
						.bootIndex(0)
						.destinationType(BDMDestType.VOLUME)
						.sourceType(BDMSourceType.VOLUME)
						.deleteOnTermination(true)
						.build());
			}
			List<String> networkIds = new ArrayList<>();
			for(ArcticTask<OSClientV3, Network> net : networks) {
				Network netObj = net.getResource();
				networkIds.add(netObj.getId());
			}
			scb.networks(networkIds);
			Server s = OSFactory.clientFromToken(getClient().getToken()).compute().servers().boot(scb.build());
			return s;
		}
			
		// Use the OpenStackWaiter class to wait or error out the building of the Server
		public void waitMethod(Server s) {
			Waiter<OSClientV3, Server> serverWaiter = OpenStackWaiter.waitForInstanceAvailable();
			try {
				serverWaiter.waitUntilReady(OSFactory.clientFromToken(getClient().getToken()), ah.getRangeId(), s, 5000, 10);
			} catch (ResourceTimeoutException e) {
				e.printStackTrace();
			} catch (ResourceErrorException e) {
				e.printStackTrace();
			}
		}
	};
		
	// Return the ArcticTask
	return server;
}
```

As you can see, it can be difficult to understand, and make, an ArcticTask. But its easier when you think about that <b>action</b> holds <b>JUST</b> making the item and <b>waitMethod</b> just holds the <b>while</b> loop to hold the thread until the resource is ready. The above waitMethod is fairly crazy, but looking into the <b>OpenStackWaiter</b> class, you'll see it's just a while loop method that checks every 10 seconds for 5000 seconds. (Lower timeInSeconds will be it errors out faster. Ensure you test this several times before attempting to merge a new provider in).
