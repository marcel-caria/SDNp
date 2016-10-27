import java.util.LinkedList;

public class Network_Hybrid extends Network_OSPF{
	
	private LinkedList<Router_OSPF_SDNenabled> hybrid_routers;
	private LinkedList<Router_OSPF> OSPF_routers;
	
	public Network_Hybrid(LinkedList<Router> routerList, String networkType){
		super(routerList, networkType);
		this.hybrid_routers = new LinkedList<Router_OSPF_SDNenabled>();
		this.OSPF_routers = new LinkedList<Router_OSPF>();
		for (Router router : this.getRouters()){
			if (!(router instanceof Router_OSPF))
				throw new Error("ERROR: All routers in the list must be of type Router_OSPF");
			this.OSPF_routers.add((Router_OSPF) router);
			if (router instanceof Router_OSPF_SDNenabled)
				this.hybrid_routers.add((Router_OSPF_SDNenabled) router);
		}
		this.computeRouting();
	}
	
	
	public Integer get_number_of_hybrid_nodes(){
		return this.hybrid_routers.size();
	}
	
	private void computeRouting(){
		for (Router_OSPF node : this.OSPF_routers)
			node.compute_all_OSPF_paths_from_here();
		for (Router_OSPF node : this.OSPF_routers)
			node.compute_all_MultiPaths_from_here();
	}
	
	public void print_all_multipaths(){
		for (Router_OSPF source : this.OSPF_routers)
			for (Router_OSPF destination : this.OSPF_routers)
				if (!source.equals(destination))
					System.out.println(source.getMultiPath(destination).toString());
	}

	public void all_multipaths_contain_OSPF_path() {
		for (Router_OSPF source : this.OSPF_routers)
			for (Router_OSPF destination : this.OSPF_routers)
				if (!source.equals(destination))
					if (!source.getMultiPath(destination).getPaths().contains(source.getPath(destination))){
						System.out.println(source.getMultiPath(destination).toString());
						System.out.println("\r\nNot contained: OSPF path " + source.getPath(destination).toString());
					}
	}
	
	public Integer number_of_hybrid_paths(){
		int numb = 0;
		for (Router_OSPF router : this.OSPF_routers)
			numb += router.number_of_hybrid_paths();
		return numb;
	}
	
}
