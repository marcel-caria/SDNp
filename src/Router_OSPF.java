import java.util.HashMap;
import java.util.LinkedList;

public class Router_OSPF extends Router{
	
	private HashMap<Router_OSPF, IP_Path> paths; // all OSPF paths from here
	private HashMap<Router_OSPF, Multi_IP_Path> multiPaths; // all hybrid MultiPaths from here
	protected HashMap<Router_OSPF, MultiMetaPath> multiMetaPaths; // all MetaPaths in an SDN-Partitioned network from here
	private boolean OSPF_paths_already_computed;
	
	public Router_OSPF(Integer number, String name) {
		super(number, name);
		this.OSPF_paths_already_computed = false;
	}
	
	public void compute_all_OSPF_paths_from_here(){
		this.paths = new HashMap<Router_OSPF, IP_Path>();
		LinkedList<Router_OSPF> currentlyProcessedNodes = new LinkedList<Router_OSPF>(this.getOSPF_Neighbors());
		for (Router_OSPF directNeighbor : currentlyProcessedNodes)
			this.paths.put(directNeighbor, new IP_Path(this, directNeighbor));
		while (!currentlyProcessedNodes.isEmpty()){
			LinkedList<Router_OSPF> to_be_processed_in_next_iteration = new LinkedList<Router_OSPF>();
			for (Router_OSPF someNode : currentlyProcessedNodes)
				for (Router_OSPF neighbor : someNode.getOSPF_Neighbors()){
					if (neighbor.equals(this))
						continue;
					IP_Path extendedPath = this.paths.get(someNode).cloneExtend(neighbor);
					if (extendedPath == null)
						continue;
					if (this.paths.containsKey(neighbor) &&
							this.paths.get(neighbor).getPathMetric() > extendedPath.getPathMetric())
						this.paths.put(neighbor, extendedPath);
					if (!this.paths.containsKey(neighbor)){
						this.paths.put(neighbor, extendedPath);
						to_be_processed_in_next_iteration.add(neighbor);
					}
				}
			currentlyProcessedNodes = to_be_processed_in_next_iteration;
		}
		this.OSPF_paths_already_computed = true;
	}
	
	// for hybrid OSPF/SDN networks
	public void compute_all_MultiPaths_from_here(){
		if (!this.OSPF_paths_already_computed)
			throw new Error("Router.compute_all_MultiPaths_from_here() called before Router.compute_all_OSPF_paths_from_here()");
		
		// initialize MultiPath map with OSPF paths
		this.multiPaths = new HashMap<Router_OSPF, Multi_IP_Path>();
		for (Router_OSPF destination : this.paths.keySet())
			this.multiPaths.put(destination, new Multi_IP_Path(this.paths.get(destination)));
		
		// add path alternatives for all destinations
		for (Router destination : this.paths.keySet())
			this.add_path_alternatives(this.paths.get(destination));
		
		// reduce number of paths
		for (Multi_IP_Path multi : this.multiPaths.values())
			multi.setK(7);
	}
	
	// for hybrid OSPF/SDN networks
	private void add_path_alternatives(IP_Path originalPath) {
		for (Router intermediateRouter : originalPath.getNodes())
			if (intermediateRouter instanceof Router_OSPF_SDNenabled && !intermediateRouter.equals(originalPath.getDestination()))
				for (Router_OSPF neighbor : ((Router_OSPF) intermediateRouter).getOSPF_Neighbors())
					if (originalPath.getHopIndex(neighbor) < 0){
						IP_Path detour = neighbor.getPath(originalPath.getDestination());
						IP_Path subpath = originalPath.subPath((Router_OSPF) originalPath.getSource(), (Router_OSPF) intermediateRouter);
						IP_Path newPath = subpath.cloneAppend(detour);
						if (newPath == null)
							continue;
						Multi_IP_Path myMulti = this.multiPaths.get(originalPath.getDestination());
						if (myMulti.isFull() && newPath.length() > myMulti.length_of_longest_path())
							continue;
						if (!myMulti.add(newPath))
							continue;
						this.add_path_alternatives(newPath);
					}
	}
	
	/**
	 * Computation of MetaPaths - Part 1:
	 * HashMap is initialized and compute_all_MetaPaths_from_here() is called.
	 */
	public void generate_MultiMetaPaths(){
		if (!(this instanceof Router_SDN_Border) && !this.OSPF_paths_already_computed)
			throw new Error("ERROR: Router.compute_all_MetaPaths_from_here() called at router " + this.getName() + " before Router.compute_all_OSPF_paths_from_here()");
		this.multiMetaPaths = new HashMap<Router_OSPF, MultiMetaPath>();
		this.compute_all_MetaPaths_from_here();
	}
	
	/**
	 * Computation of MetaPaths - Part 2:
	 * Each path from here is converted to a MetaPath.
	 * Each MetaPath is then recursively extended into a set of MetaPaths
	 * by calling the initial MetaPaths function MetaPath.extend().
	 * Finally, all generated MetaPaths are sorted into the MultiMetaPath
	 * objects according to their destination.
	 * This method is overridden in Router_SDN_Border.
	 */
	protected void compute_all_MetaPaths_from_here(){
		for (IP_Path path : this.paths.values())
			for (MetaPath mp : new MetaPath(path).extend())
				if (this.multiMetaPaths.containsKey(mp.getDestination()))
					this.multiMetaPaths.get(mp.getDestination()).add(mp);
				else
					this.multiMetaPaths.put((Router_OSPF) mp.getDestination(), new MultiMetaPath(mp));
	}
	
	/**
	 * Computation of MetaPaths - Part 4: (Part 3 is in MetaPath)
	 * This function is called from a MetaPath that ends here at this node.
	 * It provides a list of PathElements that can be used to extend the calling MetaPath.
	 * This method is overridden in Router_SDN_Border, because Router_OSPF extends only with
	 * PathElements of type IP_Path, whereas Router_SDN_Border extends only with PathElements
	 * of type Link.
	 * 
	 * @param 	metaPath	The MetaPath object which calls this function.
	 * @return	The list of PathelEments that can be used to extend the calling MetaPath object.
	 */
	public LinkedList<PathElement> getMetaPathExtensions(MetaPath metaPath) {
		if (!metaPath.getDestination().equals(this))
			return null;
		// assure that no path succeeds a path
		if (metaPath.getPathelements().getLast().isPath())
			return null;
		LinkedList<PathElement> list = new LinkedList<PathElement>();
		for (IP_Path path : this.paths.values())
			// assure that provided PathElement doesn't traverse some already traversed node
			if (disjoint(metaPath, path))
				list.add(new PathElement(path));
		return list;
	}
	
	/**
	 * Determines if a MetaPath can be extended with an IP_Path.
	 * Returns true, if both parameters are node disjoint, except for
	 * metaPath.getDestintion() and path.getSource(), which have to be identical.
	 * 
	 * @param metaPath	The preceding MetaPath.
	 * @param path		The path to be appended to metaPath.
	 * @return			True if both parameters are node disjoint
	 */
	private boolean disjoint(MetaPath metaPath, IP_Path path) {
		for (Router traversedNode : metaPath.getNodes())
			if (path.getHopIndex(traversedNode) > 0)
				return false;
		return true;
	}

	public LinkedList<Router_OSPF> getOSPF_Neighbors(){
		LinkedList<Router_OSPF> neighbors = new LinkedList<Router_OSPF>();
		for (Router router : super.getNeighbors())
			if (router instanceof Router_OSPF)
				neighbors.add((Router_OSPF) router);
		return neighbors;
	}
	
	public IP_Path getPath(Router destination){
		return this.paths.get(destination);
	}
	
	public Multi_IP_Path getMultiPath(Router destination){
		return this.multiPaths.get(destination);
	}
	
	public MultiMetaPath getMultiMetaPath(Router destination){
		return this.multiMetaPaths.get(destination);
	}
	
	public Integer number_of_hybrid_paths(){
		int numb = 0;
		for (Multi_IP_Path multi : this.multiPaths.values())
			numb += multi.size();
		return numb;
	}
	
    public String allPaths_toString(){
    	String out = "All paths from " + this.getName() + ":";
    	if (this.paths!=null)
        	for (Router dst : this.paths.keySet())
    			out += "\r\n" + this.paths.get(dst).toString();
    	return out;
    }
	
    @Override
    public Router clone(){
    	Router theclone = new Router_OSPF(this.getNumber(), this.getName());
    	return theclone;
    }
	
}