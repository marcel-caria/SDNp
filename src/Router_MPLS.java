import java.util.HashMap;
import java.util.LinkedList;

public class Router_MPLS extends Router{
	
	HashMap<Router_MPLS, Multi_MPLS_Path> multiPaths;
	
	
	public Router_MPLS(Integer number, String name) {
		super(number, name);
	}
	
	public void compute_all_paths_from_here(){
		this.multiPaths = new HashMap<Router_MPLS, Multi_MPLS_Path>();
		LinkedList<MPLS_Path> allPaths = new LinkedList<MPLS_Path>();
		for (Router neighbor : this.getNeighbors()){
			Router_MPLS neighb = (Router_MPLS) neighbor;
			MPLS_Path newPath = new MPLS_Path(this, neighb);
			Multi_MPLS_Path multi = new Multi_MPLS_Path(newPath);
			this.multiPaths.put(neighb, multi);
			allPaths.add(newPath);
		}
		this.breadthSearch(allPaths);
		for (Multi_MPLS_Path multi : this.multiPaths.values())
			multi.setK(7);
	}

	private void breadthSearch(LinkedList<MPLS_Path> paths_in_this_iteration) {
		// check end condition
		if (paths_in_this_iteration.isEmpty())
			return;
		// extend paths of last iteration
		LinkedList<MPLS_Path> nextIteration = new LinkedList<MPLS_Path>();
		for (MPLS_Path oldPath : paths_in_this_iteration)
			for (Router nextHop : oldPath.getDestination().getNeighbors())
				if (!oldPath.contains(nextHop)){
					MPLS_Path newPath = oldPath.cloneExtend((Router_MPLS) nextHop);
					if (this.add(newPath))
						nextIteration.add(newPath);
				}
		// recursion
		this.breadthSearch(nextIteration);
	}
	
	// add path to multiPaths HashMap
	private boolean add(MPLS_Path path){
		if (this.multiPaths.containsKey(path.getDestination()))
			return this.multiPaths.get(path.getDestination()).add(path);
		else
			this.multiPaths.put((Router_MPLS) path.getDestination(), new Multi_MPLS_Path(path));
		return true;
	}
	
    @Override
    public Router clone(){
    	Router theclone = new Router_OSPF(this.getNumber(), this.getName());
    	return theclone;
    }

	public Multi_MPLS_Path getMultiPath(Router destination) {
		return this.multiPaths.get(destination);
	}

	public void printAllPaths() {
		System.out.println();
		for (Router_MPLS r : this.multiPaths.keySet())
			System.out.println(this.multiPaths.get(r).toString());
	}

	public void print_paths_to_destinations_with_less_than_k_paths() {
		for (Router_MPLS r : this.multiPaths.keySet())
			if (!this.multiPaths.get(r).isFull())
				System.out.println(this.multiPaths.get(r).toString());
	}

}
