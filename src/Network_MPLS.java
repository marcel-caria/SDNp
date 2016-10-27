import java.util.LinkedList;

public class Network_MPLS extends Network{
	
	LinkedList<Router_MPLS> mpls_router;
	
	public Network_MPLS(LinkedList<Router> routerList, String networkType){
		super(routerList, networkType);
		this.mpls_router = new LinkedList<Router_MPLS>();
		for (Router router : this.getRouters())
			if (!(router instanceof Router_MPLS))
				throw new Error("ERROR: All routers in the list must be of type Router_MPLS");
			else
				this.mpls_router.add((Router_MPLS) router);
		this.computeRouting();
	}
	

	private void computeRouting(){
		for (Router_MPLS node : this.mpls_router)
			node.compute_all_paths_from_here();
	}
	
	public Multi_MPLS_Path getMultiPath(Router source, Router destination){
		if (this.getRouters().contains(source))
			return ((Router_MPLS) source).getMultiPath(destination);
		return null;
	}
	
	public void print_all_paths(){
		for (Router_MPLS r : this.mpls_router)
			r.printAllPaths();
	}

	public void print_paths_of_node_pairs_with_less_than_k_paths() {
		for (Router_MPLS r : this.mpls_router)
			r.print_paths_to_destinations_with_less_than_k_paths();
	}
	
}
