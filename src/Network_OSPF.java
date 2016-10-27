import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class Network_OSPF extends Network{
	
	private LinkedList<MetricVector> availableMetricVectors;
	
	
	public Network_OSPF(LinkedList<Router> routerList, String networkType){
		super(routerList, networkType);
		for (Router router : this.getRouters())
			if (!(router instanceof Router_OSPF))
				throw new Error("ERROR: All routers in the list must be of type Router_OSPF");
		// in case this network is a sub-domain, assure that routing computation is limited to the nodes of this network
		this.disableBorderLinks();
		this.computeRouting();
		this.enableBorderLinks();
	}
	
	
	public Result_load_balancing getUtilizationResults(){
		Result_load_balancing resultObj = new Result_load_balancing();
		for (Link link : this.getLinks()){
			Integer bandwidth = link.getBandwidth();
			Double load = link.getTrafficLoad();
			resultObj.add(load, bandwidth);
		}
		return resultObj;
	}
	
	public LinkedList<TrafficFlow> generate_scaled_random_traffic_and_assign_link_capacities(Random random , double maxLinkUtilization){
		super.generateRandomTraffic(random);
		this.rescale_traffic_and_assign_link_capacities(maxLinkUtilization);
		return this.getTraffic();
	}
	
	private LinkedList<TrafficFlow> rescale_traffic_and_assign_link_capacities(double maxLinkUtilization){
		int maxCap = 0;
		for (Integer cap : this.getAvailableLinkCapacityTypes())
			if (maxCap < cap)
				maxCap = cap;
		for (Link link : this.getLinks())
			link.setBandwidth(maxCap);
		this.compute_OSPF_link_loads();
		double factor = this.getTrafficRescalingFactor(maxLinkUtilization);
		this.rescaleTraffic(factor);
		this.compute_OSPF_link_loads();
		for (Link link : this.getLinks()){
			int minCap = 1000000000;
			for (Integer cap : this.getAvailableLinkCapacityTypes())
				if (minCap > cap && maxLinkUtilization * cap >= link.getTrafficLoad())
					minCap = cap;
			link.setBandwidth(minCap);
		}
		return this.getTraffic();
	}
	
	public void compute_OSPF_link_loads(){
		for (Link link : this.getLinks())
			link.setTrafficLoad(0.0);
		for (Router source : this.getRouters()){
			HashMap<Router, TrafficFlow> trafficMap = this.getTrafficHashMap(source);
			for (Router destination : this.getRouters())
				if (!source.equals(destination))
					for (Link link : ((Router_OSPF) source).getPath(destination).getLinks())
						link.addTrafficLoad(trafficMap.get(destination).getDemand());
		}
	}
	
	private Double getTrafficRescalingFactor(double allowedUtilization){
		return (allowedUtilization / this.getMaxUtilization());
	}
	
	private HashMap<Router, TrafficFlow> getTrafficHashMap(Router source){
		HashMap<Router, TrafficFlow> map = new HashMap<Router, TrafficFlow>();
		for (TrafficFlow flow : this.getTraffic())
			if (flow.getSource().equals(source))
				map.put(flow.getDestination(), flow);
		return map;
	}
	
	private Double getMaxUtilization(){
		Double max = 0.0;
		for (Link link : this.getLinks())
			if (max < link.getTrafficLoad() / (1.0*link.getBandwidth()))
				max = link.getTrafficLoad() / (1.0*link.getBandwidth());
		return max;
	}
	
	/**
	 * This function disables all links of border nodes that direct out of this network
	 * for the purpose of OSPF path computation.
	 */
	private void disableBorderLinks() {
		for (Router router : this.getRouters())
			if (router instanceof Router_SDN_Border)
				router.disable_all_links_except_the_ones_to(this.getRouters());
	}

	/**
	 * This function enables all links of border nodes after OSPF path computation.
	 */
	private void enableBorderLinks() {
		for (Router router : this.getRouters())
			if (router instanceof Router_SDN_Border)
				router.enableAllLinks();
	}

	private void computeRouting(){
		for (Router node : this.getRouters()){
			if (node instanceof Router_SDN_Border)
				continue;
			((Router_OSPF) node).compute_all_OSPF_paths_from_here();
		}
	}
	
	public IP_Path getPath(Router source, Router destination){
		if (this.getRouters().contains(source))
			return ((Router_OSPF) source).getPath(destination);
		return null;
	}
	
	public void print_all_paths(){
		for (Router r : this.getRouters())
			System.out.println(((Router_OSPF) r).allPaths_toString());
	}
	
	// in case this OSPF network represents a sub-domain of an SDN-partitioned network
	public LinkedList<Router_SDN_Border> getBorderNodes(){
		LinkedList<Router_SDN_Border> list = new LinkedList<Router_SDN_Border>();
		for (Router router : this.getRouters())
			if (router instanceof Router_SDN_Border)
				list.add((Router_SDN_Border) router);
		return list;
	}
	
	// in case this OSPF network represents a sub-domain of an SDN-partitioned network
	public LinkedList<Router_OSPF> getRegularNodes(){
		LinkedList<Router_OSPF> list = new LinkedList<Router_OSPF>();
		for (Router router : this.getRouters())
			if (!(router instanceof Router_SDN_Border))
				list.add((Router_OSPF) router);
		return list;
	}

	public void generateMetricVectors() {
		MetricVectorGenerator weights = new MetricVectorGenerator(this, 50);
		this.availableMetricVectors = weights.getMetricVectors();
	}
	
	public LinkedList<MetricVector> getMetricVectors(){
		return this.availableMetricVectors;
	}
	
	public void printLinkMonitoringInfo(){
		for (Link link : this.getLinks()){
			System.out.println(link.monitoringInfo());
		}
			
	}

	public void printLinkMonitoringInfo(LinearFunction cost){
		System.out.println("\r\nOSPF link utilizations:");
		double totalCost = 0.0;
		double excessDemand = 0.0;
		for (Link link : this.getLinks()){
			if (link.getTrafficLoad() > link.getBandwidth())
				excessDemand += link.getTrafficLoad() - link.getBandwidth();
			double c = link.get_OSPF_utilization_cost(cost);
			if (c < 0.0000001)
				continue;
			System.out.println(link.monitoringInfo(cost));
			totalCost += link.get_OSPF_utilization_cost(cost);
		}
		System.out.println("##################### Total Cost: " + totalCost);
		int ppm = (int) Math.round(excessDemand / this.getTotalTrafficDemand() * 1000.0);
		System.out.println("Excess traffic demand: " + excessDemand + "  (" + ppm + "ppm)");
	}
	
	public double get_OSPF_utilization_cost(LinearFunction cost){
		double totalCost = 0.0;
		for (Link link : this.getLinks())
			totalCost += link.get_OSPF_utilization_cost(cost);
		return totalCost;
	}

	public int get_total_capacity() {
		int totalCap = 0;
		for (Link link : this.getLinks())
			totalCap += link.getBandwidth();
		return totalCap;
	}

	public Result_load_balancing getUtilizationResult() {
		Result_load_balancing result = new Result_load_balancing();
		for (Link link : this.getLinks())
			result.add(link.getTrafficLoad(), link.getBandwidth());
		return result;
	}
	
	public double number_of_new_advertisements(MetricVector newVector, Router destination) {
		Router_SDN_Border bordernode = this.getBorderNodes().getFirst();
		Router_OSPF neighbor = null;
		for (Router r : bordernode.getNeighbors())
			if (!(r instanceof Router_SDN_Border) && this.getRegularNodes().contains(r))
				neighbor = (Router_OSPF) r;
		MetricVector oldVector = bordernode.get_metrics_advertised_to(neighbor).get(destination);
		return newVector.compare(oldVector);
	}


	
}
