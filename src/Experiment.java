import java.util.LinkedList;
import java.util.Random;

import gurobi.GRBException;

public class Experiment {
	
	// global objects
	private SNDlibParser parser;
	private static final double maxLinkUtilization = 0.8;
	private final Integer number_of_links;
	public static final Double gurobiMaxGap = 0.01;	// maximum allowed (objective/upperBound)
	public static final Integer gurobiTimeLimit = 0; // in seconds, set to zero for no time limit
	public static final String[] sndLibFileNames = {	".\\data\\janos-us-ca-39nodes.txt",
														".\\data\\nobel-eu-28nodes.txt",
														".\\data\\cost266-37nodes.txt",
														".\\data\\polska-12nodes.txt",
														".\\data\\2segments_trivial-6nodes.txt",
														".\\data\\3segments-9nodes.txt"};
	public static final String[] networkTypes = {"OSPF", "MPLS", "Hybrid", "Partitioned"};
	public static final Long[] randomSeeds = {	4297254836592102391L, 
												8027148154355381015L, 
												1231231231231231231L,
												6936450116485093672L,
												4978011176824433211L,
												4561348925698909809L,
												5675675675675675675L,
												1872349872349872349L,
												4560984560984560984L,
												1111111111234567890L};
	private LinkedList<Network> networks;
	private LinkedList<Network> networks_with_link_failure;
	
	// for debugging purposes
	private boolean singleNetworkScenario = false;
	private String onlyNetworkType = networkTypes[3] + "-1";
	
	// Constructor
	public Experiment(String sndLibFileName){
		this.parser = new SNDlibParser(sndLibFileName);
		this.parser.randomize_OSPF_link_weights();
		this.number_of_links = parser.getNumberOfLinks();
		if (this.singleNetworkScenario){
			this.networks = new LinkedList<Network>();
			this.networks.add(this.parser.build_OSPF_Network(null));
			this.networks.add(this.parser.build(this.onlyNetworkType, null));
		}
		else
			this.networks = this.parser.buildAllNetworks(null);
		this.networks_with_link_failure = new LinkedList<Network>();
		for (Network netw : this.networks)
			if (netw instanceof Network_SDN_Partitioned)
				((Network_SDN_Partitioned) netw).generateMetricVectors();
	}
	
	
	// Publicly accessible functions
	
	public void makeRandomTraffic(Long randomSeed){
		Random random = new Random(randomSeed);
		Network_OSPF ospf = null;
		for (Network netw : this.networks)
			if (netw.getNetworkType().equals(networkTypes[0]))
				ospf = (Network_OSPF) netw;
		ospf.generate_scaled_random_traffic_and_assign_link_capacities(random, maxLinkUtilization);
		for (Network netw : this.networks)
			if (!netw.equals(ospf))
				netw.setTraffic(ospf.getTraffic());
	}
	
	public void setLinkCapacities(){
		Network_OSPF ospf = null;
		for (Network netw : this.networks)
			if (netw.getNetworkType().equals(networkTypes[0]))
				ospf = (Network_OSPF) netw;
		for (Network netw : this.networks)
			if (!netw.equals(ospf))
				netw.setLinkCapacities(ospf.getLinks());
		for (Network netw : this.networks_with_link_failure)
			netw.setLinkCapacities(ospf.getLinks());
	}
	
	public void optimizeCapacities(){
		for (Network netw : this.networks)
			if (!netw.getNetworkType().equals(networkTypes[0]))
				if (!this.singleNetworkScenario || netw.getNetworkType().equals(this.onlyNetworkType)){
					GurobiRoutingOptimizer gurobi = new GurobiRoutingOptimizer(netw, gurobiMaxGap, gurobiTimeLimit);
					try {gurobi.minimize_capacities();}
					catch (GRBException e) {e.printStackTrace();}
				}
		for (Network netw : this.networks)
			if (netw.getNetworkType().equals(networkTypes[0]))
				System.out.println("Total capacity of the OSPF network: " + ((Network_OSPF) netw).get_total_capacity());
	}

	public void loadBalance(){
		Result_load_balancing result = null;
		for (Network netw : this.networks)
			if (!netw.getNetworkType().equals(networkTypes[0])){
				if (!this.singleNetworkScenario || netw.getNetworkType().equals(this.onlyNetworkType)){
					GurobiRoutingOptimizer gurobi = new GurobiRoutingOptimizer(netw, gurobiMaxGap, gurobiTimeLimit);
					try {result = gurobi.load_balance();}
					catch (GRBException e) {e.printStackTrace();}
					result.sndLibFileName = this.parser.getFileName();
					result.networkType = netw.getNetworkType();
					result.failedLink = "none";
					result.writeToDisk();
				}
			}
			else{
				result = ((Network_OSPF) netw).getUtilizationResult();
				result.sndLibFileName = this.parser.getFileName();
				result.networkType = netw.getNetworkType();
				result.failedLink = "none";
				result.writeToDisk();
			}
	}
	
	public void failureRecovery(){
		for (int linkIndex = 0; linkIndex < this.number_of_links; linkIndex++)
			this.failureRecovery(linkIndex);
	}
	
	public void failureRecovery(int linkIndex){
		String linkName = this.parser.getLinkNames().get(linkIndex);
		this.simulate_link_failure_on(linkName);
		this.put_traffic_in_new_network_ojects_with_link_failures();
		this.loadBalance_after_link_failure(linkName);
	}
	
	
	// Internal functions
	
	private void simulate_link_failure_on(String linkName) {
		if (this.singleNetworkScenario){
			this.networks_with_link_failure = new LinkedList<Network>();
			this.networks_with_link_failure.add(this.parser.build_OSPF_Network(linkName));
			this.networks_with_link_failure.add(this.parser.build(this.onlyNetworkType, linkName));
		}
		else
			this.networks_with_link_failure = this.parser.buildAllNetworks(linkName);
		for (Network netw_new : this.networks_with_link_failure)
			if (netw_new instanceof Network_SDN_Partitioned){
				for (Network netw_old : this.networks)
					if (netw_old.getNetworkType().equals(netw_new.getNetworkType()))
						((Network_SDN_Partitioned) netw_new).retrieve_optimum_metrics((Network_SDN_Partitioned) netw_old);
				((Network_SDN_Partitioned) netw_new).generateMetricVectors();
			}
		this.put_traffic_in_new_network_ojects_with_link_failures();
		this.setLinkCapacities();
		Network_OSPF ospf = null;
		for (Network netw : this.networks_with_link_failure)
			if (netw.getNetworkType().equals(networkTypes[0]))
				ospf = (Network_OSPF) netw;
		ospf.compute_OSPF_link_loads();
	}
	
	private void put_traffic_in_new_network_ojects_with_link_failures(){
		for (Network netw : this.networks_with_link_failure)
			netw.setTraffic(this.networks.getFirst().getTraffic());
	}
	
	private void loadBalance_after_link_failure(String linkName){
		Result_load_balancing result = null;
		for (Network netw : this.networks_with_link_failure)
			if (!netw.getNetworkType().equals(networkTypes[0])){
				if (!this.singleNetworkScenario || netw.getNetworkType().equals(this.onlyNetworkType)){
					GurobiRoutingOptimizer gurobi = new GurobiRoutingOptimizer(netw, gurobiMaxGap, gurobiTimeLimit);
					try {result = gurobi.load_balance_after_linkfailure(1.0);}
					catch (GRBException e) {e.printStackTrace();}
					result.sndLibFileName = this.parser.getFileName();
					result.networkType = netw.getNetworkType();
					result.failedLink = linkName;
					result.metricReconfigurations += netw.get_OSPF_routing_reconfigurations_due_to_a_link_failure();
					result.writeToDisk();
				}
			}
			else{
				result = ((Network_OSPF) netw).getUtilizationResult();
				result.sndLibFileName = this.parser.getFileName();
				result.networkType = netw.getNetworkType();
				result.failedLink = linkName;
				result.metricReconfigurations += netw.get_OSPF_routing_reconfigurations_due_to_a_link_failure();
				result.writeToDisk();
			}
	}
	
	
	// To execute the experiment
	public static void main(String[] args) {
		Experiment exp = new Experiment(sndLibFileNames[0]);
		exp.makeRandomTraffic(randomSeeds[0]);
		exp.setLinkCapacities();
		exp.loadBalance();
		exp.failureRecovery();
	}
	
}
