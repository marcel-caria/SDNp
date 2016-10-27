import java.util.LinkedList;

abstract class Router {
	
	private Integer number;
	private String name;
	private LinkedList<Link> links_away_from_me;
	private LinkedList<Link> links_towards_me;
	
	
	public Router(Integer number, String name) {
		this.number = number;
		this.name = name;
		this.links_towards_me = new LinkedList<Link>();
		this.links_away_from_me = new LinkedList<Link>();
	}
	
	
	public Integer getNumber() {
		return number;
	}

	public String getName() {
		return name;
	}
	
	public LinkedList<Link> getAllLinks(){
		LinkedList<Link> linkList = new LinkedList<Link>();
		for (Link link : this.links_away_from_me)
			if (link.isEnabled())
				linkList.add(link);
		for (Link link : this.links_towards_me)
			if (link.isEnabled())
				linkList.add(link);
		return linkList;
	}
	
	public LinkedList<Link> get_Out_Links(){
		LinkedList<Link> linkList = new LinkedList<Link>();
		for (Link link : this.links_away_from_me)
			if (link.isEnabled())
				linkList.add(link);
		return linkList;
	}
	
	public LinkedList<Link> get_In_Links(){
		LinkedList<Link> linkList = new LinkedList<Link>();
		for (Link link : this.links_towards_me)
			if (link.isEnabled())
				linkList.add(link);
		return linkList;
	}
	
	public boolean hasNeighbor(Router someNode){
		for (Link myLink : this.get_Out_Links())
			if (myLink.getDestination().equals(someNode))
				return true;
		return false;
	}
	
	public LinkedList<Router> getNeighbors(){
		LinkedList<Router> neighbors = new LinkedList<Router>();
		for (Link myLink : this.get_Out_Links())
			if (!neighbors.contains(myLink.getDestination()))
				neighbors.add(myLink.getDestination());
		return neighbors;
	}
	
	public Link getLink_to(Router neighbor){
		for (Link myLink : this.get_Out_Links())
			if (myLink.getDestination().equals(neighbor))
				return myLink;
		return null;
	}
	
	public Link getLink_from(Router neighbor){
		for (Link myLink : this.get_In_Links())
			if (myLink.getSource().equals(neighbor))
				return myLink;
		return null;
	}
	
	public boolean addBidirectionalLink(Link a_to_b, Link b_to_a){
		if (this.hasNeighbor(a_to_b.getDestination()) || this.hasNeighbor(b_to_a.getDestination()))
			return false;
		if (a_to_b.getSource() == this && a_to_b.getDestination() == this)
			return false;
		if (a_to_b.getSource() != this && a_to_b.getDestination() != this)
			return false;
		if (a_to_b.getSource() != b_to_a.getDestination())
			return false;
		if (a_to_b.getDestination() != b_to_a.getSource())
			return false;
		
		if (a_to_b.getSource() == this){
			this.links_away_from_me.add(a_to_b);
			this.links_towards_me.add(b_to_a);
			return true;
		}
		
		if (b_to_a.getSource() == this){
			this.links_away_from_me.add(b_to_a);
			this.links_towards_me.add(a_to_b);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Function similar to equals(), but here we disregard the type of router and compare only
	 * name and number of the router. The purpose of this function is to compare nodes from
	 * different network types.
	 * 
	 * @param other		The node to compare with.
	 * @return			True, if the router specified in the parameter represents the same
	 * 					node in a different network.
	 */
	public boolean sameNode(Router other){
//		if (other==null) return false;
		return (other.name.equals(this.name) && other.number == this.number);
	}
	
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (other.getClass() != this.getClass()) return false;
	    Router otherNode = (Router) other;
	    if (!(otherNode instanceof Router)) return false;
	    return (otherNode.getName().equals(this.name) && otherNode.getNumber().equals(this.number));
	}
	
    @Override
    public int hashCode() {
    	String identity = this.getNumber() + this.getName();
        return identity.hashCode();
    }
    
    public LinkedList<Link> disable_all_links_except_the_ones_to(LinkedList<Router> good_neighbors) {
    	LinkedList<Link> bufferList = new LinkedList<Link>();
    	LinkedList<Link> disabled = new LinkedList<Link>();
    	bufferList.addAll(this.links_away_from_me);
    	while (!bufferList.isEmpty()){
    		Link nextLink = bufferList.poll();
			if (nextLink.isEnabled() && !good_neighbors.contains(nextLink.getDestination())){
				this.disable(nextLink);
				disabled.add(nextLink);
			}
    	}
    	bufferList.addAll(this.links_towards_me);
    	while (!bufferList.isEmpty()){
    		Link nextLink = bufferList.poll();
			if (!good_neighbors.contains(nextLink.getSource())){
				this.disable(nextLink);
				disabled.add(nextLink);
			}
    	}
    	return disabled;
	}
    
    public void disable(Link link) {
		if (!this.links_away_from_me.contains(link) && !this.links_towards_me.contains(link))
			throw new Error("ERROR: Router " + this.name + " attempts to disable foreign link " + link.toString() + "!");
		link.disable();
	}
	
	public LinkedList<Link> enableAllLinks() {
		LinkedList<Link> enabled = new LinkedList<Link>();
		for (Link link : this.links_away_from_me)
			if (!link.isEnabled()){
				link.enable();
				enabled.add(link);
			}
		for (Link link : this.links_towards_me)
			if (!link.isEnabled()){
				link.enable();
				enabled.add(link);
			}
		for (Link link : this.getAllLinks())
			if (!link.isEnabled())
				throw new Error("ERROR: unable to activate link " + link.toString() + "!");
		return enabled;
	}

	
}