import java.util.LinkedList;

import gurobi.GRBVar;

public class IP_Path implements Path{
	private Integer pathMetric;
	private LinkedList<Router_OSPF> nodes;
	private LinkedList<Link> links;
	private GRBVar isUsed;
	
	
	public IP_Path(Router_OSPF sourceNode, Router_OSPF neighbor){
		if (!sourceNode.hasNeighbor(neighbor))
			throw new IllegalArgumentException(sourceNode.getName() + " and " + neighbor.getName() 
				+ " not connected, but consecutively supposed to be used in a path!");
		this.nodes = new LinkedList<Router_OSPF>();
		this.nodes.add(sourceNode);
		this.nodes.add(neighbor);
		this.links = new LinkedList<Link>();
		this.links.add(sourceNode.getLink_to(neighbor));
		this.pathMetric = sourceNode.getLink_to(neighbor).getWeight();
	}
	
	/**
	 * Internal constructor for a single node.
	 * 
	 * @param	singleNode	the only node on this path
	 * @return				the according Path object
	 */
	private IP_Path(Router_OSPF singleNode){
		this.nodes = new LinkedList<Router_OSPF>();
		this.nodes.add(singleNode);
		this.links = new LinkedList<Link>();
		this.pathMetric = 0;
	}
	
	
	public GRBVar get_GUROBI_Variable(){
		return this.isUsed;
	}
	
	public void assign_GUROBI_Variable(GRBVar variable){
		this.isUsed = variable;
	}

	@Override
	public Integer length(){
		return this.nodes.size();
	}
	
	@Override
	public int getHopIndex(Router router){
		if (!this.nodes.contains(router))
			return -1;
		return this.nodes.indexOf(router);
	}
	
	@Override
	public LinkedList<Link> getLinks() {
		return new LinkedList<Link>(this.links);
	}
	
	public IP_Path subPath(Router_OSPF newSource, Router_OSPF newDestination){
		if (!this.nodes.contains(newSource) || !this.nodes.contains(newDestination))
			return null;
		int srcIndex = this.nodes.indexOf(newSource);
		int dstIndex = this.nodes.indexOf(newDestination);
		if (srcIndex > dstIndex)
			return null;
		if (srcIndex == dstIndex)
			return new IP_Path(newSource);
		IP_Path subpath = new IP_Path(this.nodes.get(srcIndex), this.nodes.get(srcIndex+1));
		for (int index = srcIndex + 2; index <= dstIndex; index++)
			subpath.add(this.nodes.get(index));
		return subpath;
	}
	
	@Override
	public Router getNode(int index){
		if (index > this.length() - 1)
			throw new IllegalArgumentException("Index out of range, this path has no router at index " + index + "!");
		return this.nodes.get(index);
	}
	
	public Integer getPathMetric() {
		return pathMetric;
	}
	
	@Override
	public Router getSource(){
		return this.nodes.getFirst();
	}
	
	@Override
	public Router getDestination(){
		return this.nodes.getLast();
	}
	
	@Override
	public LinkedList<Router> getNodes() {
		return new LinkedList<Router>(this.nodes);
	}

	// returns a copy of this path with an additional hop
	public IP_Path cloneExtend(Router_OSPF nextHop){
		IP_Path extended = this.clone();
		if (!extended.add(nextHop))
			return null;
		return extended;
	}
	
	// returns a copy of this path with otherPath appended
	public IP_Path cloneAppend(IP_Path otherPath) {
		IP_Path newPath = this.clone();
		for (Router nextHop : otherPath.getNodes())
			if (!newPath.add((Router_OSPF) nextHop))
				return null;
		return newPath;
	}
	
	// this is the only function that directly modifies the nodes list
	private boolean add(Router_OSPF nextHop){
		if (this.nodes.contains(nextHop))
			return false;
		if (!this.nodes.getLast().hasNeighbor(nextHop))
			return false;
		this.pathMetric += this.nodes.getLast().getLink_to(nextHop).getWeight();
		this.links.add(this.nodes.getLast().getLink_to(nextHop));
		this.nodes.add(nextHop);
		return true;
	}
	
	@Override
	public IP_Path clone(){
		if (this.nodes.size()==1)
			return new IP_Path(this.nodes.getFirst());
		IP_Path theclone = new IP_Path(this.nodes.get(0), this.nodes.get(1));
		for (int i=2; i<this.nodes.size(); i++)
			theclone.add(this.nodes.get(i));
		return theclone;
	}
	
	@Override
	public String toString(){
		String out = "";
		for (Router_OSPF node : this.nodes)
			if (node instanceof Router_OSPF_SDNenabled)
				out += "{" + node.getName() + "} ";
			else
				out += node.getName() + " ";
		return out;
	}
	
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (other.getClass() != this.getClass()) return false;
	    IP_Path otherPath = (IP_Path) other;
	    if (otherPath.length() != this.length()) return false;
	    for (int i=0; i<this.length(); i++)
	    	if (!otherPath.getNode(i).equals(this.getNode(i)))
	    		return false;
	    return true;
	}
	
    @Override
    public int hashCode() {
    	return this.getNodes().hashCode();
    }

}
