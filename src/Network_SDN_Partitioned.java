import java.util.LinkedList;

public class Network_SDN_Partitioned extends Network{
	
	private LinkedList<Network_OSPF> subdomains;
	private LinkedList<Router_OSPF> OSPF_nodes; // all nodes in this network
	private LinkedList<Router_SDN_Border> borderNodes; // subset of OSPF_nodes
	
	public Network_SDN_Partitioned(LinkedList<Router> routers, String networkType) {
		super(routers, networkType);
		System.out.print("Building SDN-partitioned network");
		this.OSPF_nodes = new LinkedList<Router_OSPF>();
		this.borderNodes = new LinkedList<Router_SDN_Border>();
		for (Router router : this.getRouters()){
			if (!(router instanceof Router_OSPF))
				throw new Error("ERROR: All routers in the list must be of type Router_OSPF");
			this.OSPF_nodes.add((Router_OSPF) router);
			if (router instanceof Router_SDN_Border)
				this.borderNodes.add((Router_SDN_Border) router);
		}
		for (Router r : this.borderNodes)
			System.out.print(" " + r.getName());
		System.out.println();
		this.generate_subdomain_network_objects();
		this.provide_MetaTopology_to_BorderNodes();
		this.compute_MultiMetaPaths();
	}
	
	
	public Integer get_number_of_bordernodes(){
		return this.borderNodes.size();
	}
	
	/**
	 * Function to pass optimum link metrics before link failure to the network object
	 * after link failure.
	 * 
	 * @param before_linkfailure	The network object that represents the SDN-partitioned
	 * 								network before the link failure occurred.
	 */
	public void retrieve_optimum_metrics(Network_SDN_Partitioned before_linkfailure){
		for (Router_SDN_Border foreignNode : before_linkfailure.get_bordernodes())
			foreignNode.check_if_advertisementMap_is_OK();
		for (Router_SDN_Border foreignNode : before_linkfailure.get_bordernodes())
			for (Router_SDN_Border myNode : this.borderNodes)
				if (foreignNode.sameNode(myNode))
					myNode.retrieve_optimum_metrics(foreignNode, this.getRouters());
	}
	
	public LinkedList<Router_SDN_Border> get_bordernodes(){
		return new LinkedList<Router_SDN_Border>(this.borderNodes);
	}
	
	public LinkedList<Network_OSPF> getSubdomains(){
		return this.subdomains;
	}
	
	private void provide_MetaTopology_to_BorderNodes() {
		for (Router_SDN_Border bordernode : this.borderNodes)
			bordernode.setMetaTopology(this.subdomains);
	}
	
	public Network subdomain_of(Router router){
		if (router instanceof Router_SDN_Border)
			return null;
		if (router instanceof Router_OSPF)
			for (Network_OSPF subdomain : this.subdomains)
				if (subdomain.getRouters().contains(router))
					return subdomain;
		return null;
	}
	
	private void generate_subdomain_network_objects() {
		this.subdomains = new LinkedList<Network_OSPF>();
		LinkedList<Router> unassigned_OSPF_routers = new LinkedList<Router>(this.OSPF_nodes);
		unassigned_OSPF_routers.removeAll(this.borderNodes);
		while (!unassigned_OSPF_routers.isEmpty())
			this.subdomains.add(this.getNextSubdomain(unassigned_OSPF_routers));
	}
	
	/**
	 * Generates an object of type Network_OSPF that represents a sub-domain.
	 * The nodes of the sub-domain are determined by using the first yet unassigned
	 * router to find all nodes in that sub-domain based on a breadth-first search.
	 * 
	 * @param unassigned_OSPF_routers	The list of routers that are yet not assigned to some sub-domain.
	 * @return	The network object that represents the sub-domain.
	 */
	private Network_OSPF getNextSubdomain(LinkedList<Router> unassigned_OSPF_routers) {
		LinkedList<Router> nodes_in_next_subdomain = new LinkedList<Router>();
		nodes_in_next_subdomain.add(unassigned_OSPF_routers.getFirst());
		this.breadthSearch(nodes_in_next_subdomain, null);
		unassigned_OSPF_routers.removeAll(nodes_in_next_subdomain);
		return new Network_OSPF(nodes_in_next_subdomain, "OSPF-Subdomain");
	}
	
	/**
	 * Recursive function to add neighbors to the list lastIteration until it contains the whole sub-domain.
	 * It adds border nodes to the list, but not their neighbors, to avoid searching across sub-domain borders.
	 * The function internally uses three lists to avoid checking nodes for unprocessed neighbors, when they
	 * have been found by the breadth-search algorithm in previous iterations.
	 * ATTENTION: initialize this function with the list lastIteration containing only a single node, and
	 * the list currentIteration being null!
	 *  
	 * @param lastIteration			list of nodes that should not be processed anymore
	 * @param currentIteration		list of nodes whose neighbors have to be processed
	 */
	private void breadthSearch(LinkedList<Router> lastIteration, LinkedList<Router> currentIteration){
		LinkedList<Router> nextIteration = new LinkedList<Router>();
		// check initialization condition
		if (currentIteration == null){
			currentIteration = new LinkedList<Router>(lastIteration);
			lastIteration.clear();
		}
		for (Router router : currentIteration)
			if (!(router instanceof Router_SDN_Border)) // don't add neighbors of border nodes!
				for (Router neighbor : router.getNeighbors())
					if (!lastIteration.contains(neighbor))
						if (!currentIteration.contains(neighbor))
							if (!nextIteration.contains(neighbor))
								nextIteration.add(neighbor);
		lastIteration.addAll(currentIteration);
		//check end condition
		if (!nextIteration.isEmpty())
			this.breadthSearch(lastIteration, nextIteration);
	}

	private void compute_MultiMetaPaths(){
		for (Router_OSPF router : this.OSPF_nodes)
			router.generate_MultiMetaPaths();
	}
	
	public void generateMetricVectors(){
		for (Network_OSPF netw : this.subdomains)
			netw.generateMetricVectors();
	}
	
	public void print_all_OSPF_paths(){
		for (Network_OSPF netw : this.subdomains){
			System.out.println("\r\n\r\nSub-Domain " + this.subdomains.indexOf(netw) + ":");
			netw.print_all_paths();
		}
	}
	
	public void print_all_MetaPaths(){
		for (Router_OSPF source : this.OSPF_nodes)
			for (Router_OSPF destination : this.OSPF_nodes)
				if (!source.equals(destination)){
					System.out.println("\r\n\r\nAll MetaPaths from " + source.getName() + " to " + destination.getName() + ":");
					for (Path p : source.getMultiMetaPath(destination).getPaths()){
						MetaPath mp = (MetaPath) p;
						System.out.println(mp.toString());
					}
				}
	}
	
	public void print_MetaPath_count(){
		for (Router_OSPF source : this.OSPF_nodes)
			for (Router_OSPF destination : this.OSPF_nodes)
				if (!source.equals(destination))
					System.out.println(source.getMultiMetaPath(destination).size() + " MetaPaths from " + source.getName() + " to " + destination.getName() + ".");
	}


	
	// if this object represents the network with the link failure
	public void count_routing_reconfigurations_for(String failedLink) {
		if (failedLink == null)
			return;
		String[] nodeNames = failedLink.split("-");
		Router node1 = null, node2 = null;
		for (Router r : this.getRouters()){
			if (r.getName().equals(nodeNames[0]))
				node1 = r;
			if (r.getName().equals(nodeNames[1]))
				node2 = r;
		}
		int size_of_subdomain_with_node1 = 0, size_of_subdomain_with_node2 = 0;
		if (!(node1 instanceof Router_SDN_Border))
			for (Network_OSPF subdomain : this.subdomains)
				if (subdomain.getRegularNodes().contains(node1))
					size_of_subdomain_with_node1 = subdomain.getRegularNodes().size();
		if (!(node2 instanceof Router_SDN_Border))
			for (Network_OSPF subdomain : this.subdomains)
				if (subdomain.getRegularNodes().contains(node2))
					size_of_subdomain_with_node2 = subdomain.getRegularNodes().size();
		this.set_OSPF_routing_reconfigurations_due_to_a_link_failure(size_of_subdomain_with_node1 * size_of_subdomain_with_node2);
	}
	
}
