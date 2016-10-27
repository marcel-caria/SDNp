import java.util.HashMap;
import java.util.LinkedList;

/**
 * Objects of this class belong to SDN-partitioned networks.
 * However, they also belong to each sub-domain to which they
 * are connected to. For OSPF path computation purposes, links
 * that direct out of the currently path-computed sub-domain
 * can be temporarily deactivated.
 */
public class Router_SDN_Border extends Router_OSPF{
	
	private LinkedList<Network_OSPF> subdomains;
	private LinkedList<Network_OSPF> subdomains_connected_to_me;
	
	// To store the optimum metrics before a link fails:
	// Access is HashMap<myNeighbor, HashMap<destination, MetricVector>>
	HashMap<Router, HashMap<Router, MetricVector>> advertisedMetrics;


	public Router_SDN_Border(Integer number, String name) {
		super(number, name);
	}
	
	
    public boolean has_old_metrics() {
    	if (this.advertisedMetrics==null)
    		return false;
    	if (this.advertisedMetrics.isEmpty()){
    		System.out.println("###########################################################################");
    		System.out.println("WARNING: No old metric vectors available!");
    		return false;
    	}
    	return true;
	}
    
    public void check_if_advertisementMap_is_OK(){
		for (Router ownNeighbor : this.getNeighbors())
			if (!this.advertisedMetrics.containsKey(ownNeighbor))
				System.out.println("###################################### WARNING: " + this.getName() 
						+ " has not advertised metrics to " + ownNeighbor.getName());
    }
    
	public HashMap<Router, MetricVector> get_metrics_advertised_to(Router foreignNeighbor) {
		for (Router ownNeighbor : this.getNeighbors())
			if (ownNeighbor.sameNode(foreignNeighbor))
				return advertisedMetrics.get(ownNeighbor);
		throw new Error();
	}
	
	// used by a MetricVectorGenerator object to retrieve all old metrics it has to provide after link failure
	public LinkedList<MetricVector> get_old_metrics_advertised_to(Router neighbor){
		if (this.advertisedMetrics.isEmpty())
			return null;
		LinkedList<MetricVector> list = new LinkedList<MetricVector>();
		for (MetricVector vector : this.advertisedMetrics.get(neighbor).values()){
			boolean usethis = true;
			for (MetricVector other : list)
				if (vector.sameRouting(other)){
					usethis = false;
					break;
				}
			if (usethis)
				list.add(vector);
		}
		return list;
	}
	
	/**
	 * Function called after link failure by the border node of the newly generated network object to retrieve
	 * the previously advertised link metrics.
	 * 
	 * @param foreignBorderNode			This is the object to retrieve the old link metrics from. It represents
	 * 									the same node as this object, but in the network object before link failure.
	 * @param nodes_in_own_network		All nodes in the network object after link failure.
	 */
	public void retrieve_optimum_metrics(Router_SDN_Border foreignBorderNode, LinkedList<Router> nodes_in_own_network) {
		System.out.println(this.getName() + " retrieves optimum metrics from " + foreignBorderNode.getName());
		this.advertisedMetrics = new HashMap<Router, HashMap<Router, MetricVector>>();
		for (Router ownNeighbor : this.getNeighbors()){
			if (ownNeighbor instanceof Router_SDN_Border)
				continue;
			System.out.print("Metrics advertised to " + ownNeighbor.getName() + ": ");
			HashMap<Router, MetricVector> foreignMap = foreignBorderNode.get_metrics_advertised_to(ownNeighbor);
			System.out.println(foreignMap.keySet().size());
			HashMap<Router, MetricVector> ownMap = new HashMap<Router, MetricVector>();
			this.advertisedMetrics.put(ownNeighbor, ownMap);
			for (Router foreignDestination : foreignMap.keySet()){
				MetricVector vector = foreignMap.get(foreignDestination);
				for (Router ownDestination : nodes_in_own_network)
					if (ownDestination.sameNode(foreignDestination))
						this.advertisedMetrics.get(ownNeighbor).put(ownDestination, vector);
			}
		}
	}
	
	// used by GUROBI to store optimum metrics
	public void setMetric(Network subdomain , Router destination, MetricVector vector){
		if (this.advertisedMetrics == null)
			this.advertisedMetrics = new HashMap<Router, HashMap<Router, MetricVector>>();
		for (Router neighbor : this.getNeighbors())
			if (!(neighbor instanceof Router_SDN_Border) && subdomain.getRouters().contains(neighbor)){
				if (!this.advertisedMetrics.keySet().contains(neighbor))
					this.advertisedMetrics.put(neighbor, new HashMap<Router, MetricVector>());
				this.advertisedMetrics.get(neighbor).put(destination, vector);
			}
	}
	
	void print_optimum_metrics(){
		System.out.println("\r\n################################### Optimum metrics stored in " + this.getName());
		for (Router neighbor : this.advertisedMetrics.keySet()){
			System.out.println("Advertised to " + neighbor.getName());
			for (Router destination : this.advertisedMetrics.get(neighbor).keySet())
				System.out.println(this.advertisedMetrics.get(neighbor).get(destination).toString());
		}
	}
	
	public Network_OSPF getSubdomain(Router router){
		if (router instanceof Router_SDN_Border)
			return null;
		for (Network_OSPF subdomain : this.subdomains)
			if (subdomain.getRouters().contains(router))
				return subdomain;
		return null;
	}
	
	@Override
	protected void compute_all_MetaPaths_from_here(){
		for (Link link : this.get_Out_Links())
			for (MetaPath mp : new MetaPath(link).extend())
				if (this.multiMetaPaths.containsKey(mp.getDestination()))
					this.multiMetaPaths.get(mp.getDestination()).add(mp);
				else
					this.multiMetaPaths.put((Router_OSPF) mp.getDestination(), new MultiMetaPath(mp));
	}
	
	@Override
	public LinkedList<PathElement> getMetaPathExtensions(MetaPath metaPath) {
		if (!metaPath.getDestination().equals(this))
			return null;
		LinkedList<PathElement> list = new LinkedList<PathElement>();
		for (Link link : this.get_Out_Links()){
			Router_OSPF neighbor = (Router_OSPF) link.getDestination();
			// assure that the next hop has yet not been traversed and does not belong to any yet traversed sub-domain
			if (metaPath.getHopIndex(neighbor) < 0 && !metaPath.traverses(this.getSubdomain(neighbor)))
				list.add(new PathElement(link));
		}
		return list;
	}
	
	public void setMetaTopology(LinkedList<Network_OSPF> subdomains){
		this.subdomains = subdomains;
		this.subdomains_connected_to_me = new LinkedList<Network_OSPF>();
		for (Network_OSPF subdomain : subdomains)
			if (subdomain.getRouters().contains(this))
				this.subdomains_connected_to_me.add(subdomain);
	}
	
    @Override
    public Router clone(){
    	Router theclone = new Router_OSPF(this.getNumber(), this.getName());
    	return theclone;
    }


	
}