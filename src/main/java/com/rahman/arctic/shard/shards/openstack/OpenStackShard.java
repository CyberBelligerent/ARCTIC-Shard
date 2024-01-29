package com.rahman.arctic.shard.shards.openstack;

import java.util.ArrayList;
import java.util.List;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.BDMDestType;
import org.openstack4j.model.compute.BDMSourceType;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.openstack.OSFactory;

import com.rahman.arctic.shard.exceptions.ResourceErrorException;
import com.rahman.arctic.shard.exceptions.ResourceTimeoutException;
import com.rahman.arctic.shard.objects.ArcticHost;
import com.rahman.arctic.shard.objects.ArcticNetwork;
import com.rahman.arctic.shard.objects.ArcticTask;
import com.rahman.arctic.shard.shards.ShardProviderTmpl;
import com.rahman.arctic.shard.shards.Waiter;

public class OpenStackShard extends ShardProviderTmpl<OSClientV3> {

	public OpenStackShard() {
		super("openstack");
	}

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

	@SuppressWarnings("unchecked")
	@Override
	protected ArcticTask<OSClientV3, Server> buildHost(ArcticHost ah) {
		List<ArcticTask<OSClientV3, Volume>> volumes = new ArrayList<>();
		List<ArcticTask<OSClientV3, Network>> networks = new ArrayList<>();
		List<ArcticTask<OSClientV3, ?>> depends = new ArrayList<>();
		ah.getNetworks().forEach(e -> {
			networks.add((ArcticTask<OSClientV3, Network>) getNetworkTasks().get(e));
			depends.add(getNetworkTasks().get(e));
		});
		
		ah.getVolumes().forEach(e -> {
			volumes.add((ArcticTask<OSClientV3, Volume>) getVolumeTasks().get(e));
			depends.add(getVolumeTasks().get(e));
		});
		
		ArcticTask<OSClientV3, Server> server = new ArcticTask<OSClientV3, Server>(10, depends) {
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
		
		return server;
	}
	
	@Override
	protected ArcticTask<OSClientV3, Network> buildNetwork(ArcticNetwork an) {
		// TODO Auto-generated method stub
		return null;
	}

}