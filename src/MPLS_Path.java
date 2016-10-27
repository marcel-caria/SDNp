import java.util.LinkedList;

import gurobi.GRBVar;

public class MPLS_Path implements Path{
	
	private LinkedList<Router> nodes;
	private LinkedList<Link> links;
	private GRBVar isUsed;

	private MPLS_Path(Router sourceNode) {
		this.nodes = new LinkedList<Router>();
		this.nodes.add(sourceNode);
		this.links = new LinkedList<Link>();
	}

	public MPLS_Path(Router router1, Router router2) {
		this.nodes = new LinkedList<Router>();
		this.nodes.add(router1);
		this.nodes.add(router2);
		this.links = new LinkedList<Link>();
		this.links.add(router1.getLink_to(router2));
	}


	@Override
	public Integer length() {
		return this.nodes.size();
	}
	
	public boolean contains(Router router){
		return this.nodes.contains(router);
	}
	
	@Override
	public int getHopIndex(Router router) {
		if (!this.nodes.contains(router))
			return -1;
		return this.nodes.indexOf(router);
	}

	@Override
	public LinkedList<Link> getLinks() {
		return new LinkedList<Link>(this.links);
	}

	@Override
	public Router getNode(int index) {
		return this.nodes.get(index);
	}

	@Override
	public Router getSource() {
		return this.nodes.getFirst();
	}

	@Override
	public Router getDestination() {
		return this.nodes.getLast();
	}

	@Override
	public LinkedList<Router> getNodes() {
		return new LinkedList<Router>(this.nodes);
	}
	
	@Override
	public MPLS_Path clone(){
		if (this.nodes.size()==1)
			return new MPLS_Path(this.nodes.getFirst());
		MPLS_Path theclone = new MPLS_Path(this.nodes.get(0), this.nodes.get(1));
		for (int i=2; i<this.nodes.size(); i++)
			theclone.add(this.nodes.get(i));
		return theclone;
	}
	
	public MPLS_Path cloneExtend(Router_MPLS nextHop){
		if (!this.getDestination().getNeighbors().contains(nextHop))
			throw new Error("ERROR: Path " + this.toString() + " can not be extended with " + nextHop.getName() + "!");
		MPLS_Path theclone = this.clone();
		theclone.nodes.add(nextHop);
		return theclone;
	}
	
	private void add(Router router) {
		this.links.add(this.nodes.getLast().getLink_to(router));
		this.nodes.add(router);
	}

	@Override
	public String toString(){
		String out = "";
		for (Router node : this.nodes)
			out += node.getName() + " ";
		return out;
	}
	
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (other.getClass() != this.getClass()) return false;
	    MPLS_Path otherPath = (MPLS_Path) other;
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


    @Override
	public GRBVar get_GUROBI_Variable() {
		return this.isUsed;
	}

	@Override
	public void assign_GUROBI_Variable(GRBVar variable) {
		this.isUsed = variable;
	}
	
}
