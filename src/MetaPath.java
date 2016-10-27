import java.util.LinkedList;

import gurobi.GRBVar;

public class MetaPath implements Path{
	
	private LinkedList<PathElement> pathelements;
	private GRBVar isUsed;
	
	public MetaPath(IP_Path path){
		this.pathelements = new LinkedList<PathElement>();
		this.pathelements.add(new PathElement(path));
	}
	
	public MetaPath(Link link){
		this.pathelements = new LinkedList<PathElement>();
		this.pathelements.add(new PathElement(link));
	}
	
	private MetaPath(){
		this.pathelements = new LinkedList<PathElement>();
	}
	
	
	public GRBVar get_GUROBI_Variable(){
		return this.isUsed;
	}
	
	public void assign_GUROBI_Variable(GRBVar variable){
		this.isUsed = variable;
	}
	
	public LinkedList<PathElement> getPathelements() {
		return pathelements;
	}

	@Override
	public Integer length() {
		int len = 0;
		for (PathElement pE : this.pathelements)
			len += pE.length() - 1;
		return len + 1;
	}

	@Override
	public int getHopIndex(Router router) {
		int index = 0;
		for (PathElement pE : this.pathelements)
			if (pE.isLink()){
				if (pE.getLink().getSource().equals(router))
					return index;
				index++;
			}
			else if (pE.isPath()){
				for (Router node_on_this_path : pE.getPath().getNodes()){
					if (node_on_this_path.equals(router))
						return index;
					index++;
				}
			}
		return -1;
	}

	@Override
	public LinkedList<Link> getLinks() {
		LinkedList<Link> allLinks = new LinkedList<Link>();
		for (PathElement pE : this.pathelements)
			if (pE.isLink())
				allLinks.add(pE.getLink());
			else if (pE.isPath())
				allLinks.addAll(pE.getPath().getLinks());
			else return null;
		return allLinks;
	}

	@Override
	public Router getNode(int index) {
		int nodeCount = 0;
		for (PathElement pE : this.pathelements)
			if (pE.isLink()){
				if (nodeCount == index)
					return pE.getLink().getSource();
				nodeCount++;
			}
			else if (pE.isPath()){
				for (Router node_on_this_path : pE.getPath().getNodes()){
					if (nodeCount == index)
						return node_on_this_path;
					nodeCount++;
				}
			}
		return null;
	}

	@Override
	public Router getSource() {
		if (this.pathelements.getFirst().isLink())
			return this.pathelements.getFirst().getLink().getSource();
		if (this.pathelements.getFirst().isPath())
			return this.pathelements.getFirst().getPath().getSource();
		return null;
	}

	@Override
	public Router getDestination() {
		if (this.pathelements.getLast().isLink())
			return this.pathelements.getLast().getLink().getDestination();
		if (this.pathelements.getLast().isPath())
			return this.pathelements.getLast().getPath().getDestination();
		return null;
	}

	@Override
	public LinkedList<Router> getNodes() {
		LinkedList<Router> nodes = new LinkedList<Router>();
		for (PathElement pE: this.pathelements)
			if (pE.isLink())
				nodes.add(pE.getLink().getSource());
			else if (pE.isPath())
				for (int i=0; i<pE.getPath().length()-1; i++)
					nodes.add(pE.getPath().getNode(i));
		nodes.add(this.pathelements.getLast().getDestination());
		return nodes;
	}
	
	/**
	 * Computation of MetaPaths - Part 3: (Parts 1 & 2 are in Router_OSPF)
	 * Recursive function to let all possible MetaPaths grow out of an initial one.
	 * Uses Router_OSPF.getMetaPathExtensions() to retrieve all possible PathElements
	 * from the current destination that can be used to extend this MetaPath.
	 * 
	 * @return	List of recursively generated MetaPaths.
	 */
	public LinkedList<MetaPath> extend(){
		LinkedList<MetaPath> list = new LinkedList<MetaPath>();
		list.add(this);
		Router_OSPF lastHop = (Router_OSPF) this.getDestination();
		LinkedList<PathElement> extensions = lastHop.getMetaPathExtensions(this);
		if (extensions == null)
			return list;
		for (PathElement pE : extensions)
			list.addAll(this.cloneExtend(pE).extend());
		return list;
	}
	
	private boolean add(PathElement pE){
		if (pE.isPath() && this.pathelements.getLast().isPath())
			return false;
		if (!pE.getSource().equals(this.pathelements.getLast().getDestination()))
			return false;
		this.pathelements.add(pE);
		return true;
	}
	
	public MetaPath cloneExtend(PathElement element){
		MetaPath theclone = this.clone();
		theclone.add(element);
		return theclone;
	}
	
	public MetaPath cloneExtend(IP_Path path){
		MetaPath theclone = this.clone();
		if (!theclone.add(new PathElement(path)))
			throw new Error("ERROR: can not append path " + path.toString() + " to MetaPath " + theclone.toString());;
		return theclone;
	}
	
	public MetaPath cloneExtend(Link link){
		MetaPath theclone = this.clone();
		if (!theclone.add(new PathElement(link)))
			throw new Error("ERROR: can not append link " + link.toString() + " to MetaPath " + theclone.toString());;
		return theclone;
	}
	
	@Override
	public MetaPath clone(){
		MetaPath theclone = new MetaPath();
		theclone.pathelements.addAll(this.pathelements);
		return theclone;
	}
	
	@Override
	public String toString(){
		String out = "";
		for (PathElement pE: this.pathelements)
			if (pE.isLink())
				out += pE.toString() + " ";
			else
				out += pE.toString();
		return out;
	}
	
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (other.getClass() != this.getClass()) return false;
	    MetaPath otherMetaPath = (MetaPath) other;
	    if (otherMetaPath.length() != this.length()) return false;
	    for (int i=0; i<this.pathelements.size(); i++)
	    	if (!otherMetaPath.pathelements.get(i).equals(this.pathelements.get(i)))
	    		return false;
	    return true;
	}
	
    @Override
    public int hashCode() {
    	return this.getNodes().hashCode();
    }
    
    /**
     * Determines whether this MetaPath object requires the advertisement of a MetricVector in the specified sub-domain.
     * 
     * @param subdomain		The sub-domain that is controlled for traversal of this MetaPath
     * @return				True, if this MetaPath traverses (but exits) the specified sub-domain
     */
    public boolean requires_MetricVector_in(Network_OSPF subdomain){
    	for (PathElement pE : this.pathelements)
    		if (!pE.equals(this.pathelements.getLast()) 
    				&& pE.isPath()
    				&& subdomain.getRouters().contains(pE.getSource()))
    			return true;
    	return false;
    }
    
    /**
     * Used by Router_SDN_Border.getMetaPathExtensions(Router_SDN_Border node) to determine if this MetaPath can
     * be extended with a neighbor of the node. This is true only if the neighbor doesn't belong to an already
     * traversed sub-domain
     * @param subdomain
     * @return
     */
    public boolean traverses(Network_OSPF subdomain){
    	if (subdomain == null)
    		return false;
    	for (PathElement pE : this.pathelements)
    		if (pE.isPath() && subdomain.getRouters().contains(pE.getSource()))
    			return true;
    	return false;
    }
    
	/**
	 * Returns true if this MetaPath can be extended with additional PathElements.
	 */
	public boolean isExtendable() {
		if (this.pathelements.getLast().isLink())
			return true;
		return this.getDestination() instanceof Router_SDN_Border;
	}
    
}
