import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import gurobi.GRBVar;

public class Network {
	
	private final String networkType;
	private LinkedList<Router> routers;
	private LinkedList<Link> links;
	private final Integer[] linkCapacityTypes = {10, 40, 100};
	private LinkedList<TrafficFlow> traffic;
	private GRBVar totalCapacity;
	private Integer OSPF_routing_reconfigurations_due_to_a_link_failure;
	
	public Integer get_OSPF_routing_reconfigurations_due_to_a_link_failure() {
		return OSPF_routing_reconfigurations_due_to_a_link_failure;
	}

	public void set_OSPF_routing_reconfigurations_due_to_a_link_failure(Integer count) {
		OSPF_routing_reconfigurations_due_to_a_link_failure = count;
	}

	public String getNetworkType() {
		return networkType;
	}
	
	public GRBVar get_GUROBI_Variable(){
		return this.totalCapacity;
	}
	
	public void assign_GUROBI_Variable(GRBVar variable){
		this.totalCapacity = variable;
	}

	public LinkedList<TrafficFlow> getTraffic() {
		return traffic;
	}
	
	Double getTotalTrafficDemand(){
		double total = 0.0;
		for (TrafficFlow flow : this.traffic)
			total += flow.getDemand();
		return total;
	}
	
	public void setTraffic(LinkedList<TrafficFlow> traffic) {
		int number_of_pairs = this.routers.size() * (this.routers.size() - 1);
		if (traffic.size() != number_of_pairs)
			throw new Error("ERROR: " + traffic.size() + " traffic flows, but " + number_of_pairs + " source-destination router pairs!");
		this.traffic = new LinkedList<TrafficFlow>();
		HashMap<Router, Router> sameNode = new HashMap<Router, Router>();
		LinkedList<TrafficFlow> buffer = new LinkedList<TrafficFlow>(traffic);
		while (!buffer.isEmpty()){
			TrafficFlow nextFlow = buffer.poll();
			Router other = nextFlow.getSource();
			if (sameNode.keySet().contains(other))
				continue;
			for (Router own : this.routers)
				if (own.sameNode(other))
					sameNode.put(other, own);
		}
		for (TrafficFlow flow : traffic)
			this.traffic.add(new TrafficFlow(flow.getDemand(), sameNode.get(flow.getSource()), sameNode.get(flow.getDestination())));
	}
	
	public void setLinkCapacities(LinkedList<Link> foreignLinks) {
		for (Link otherLink : foreignLinks)
			for (Link myLink : this.links)
				if (otherLink.sameLink(myLink))
					myLink.setBandwidth(otherLink.getBandwidth());
	}
	
	public LinkedList<TrafficFlow> generateRandomTraffic(Random random){
		this.traffic = new LinkedList<TrafficFlow>();
		for (Router src : this.routers)
			for (Router dst : this.routers)
				if (!src.equals(dst))
					this.traffic.add(new TrafficFlow(random.nextDouble(), src, dst));
		return this.traffic;
	}

	void rescaleTraffic(double scalingFactor){
		for (TrafficFlow flow : this.traffic)
			flow.rescaleDemand(scalingFactor);
	}

	public Network(LinkedList<Router> routers, String networkType){
		this.networkType = networkType;
		this.routers = routers;
		this.links = new LinkedList<Link>();
		for (Router nextRouter : routers)
			for (Link nextLink : nextRouter.getAllLinks())
				if (!this.links.contains(nextLink))
					this.links.add(nextLink);
	}

	public LinkedList<Router> getRouters() {
		return this.routers;
	}

	public LinkedList<Link> getLinks() {
		return links;
	}
	
	public Integer[] getAvailableLinkCapacityTypes(){
		return this.linkCapacityTypes;
	}
	
}
