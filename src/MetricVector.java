import java.util.HashMap;
import java.util.LinkedList;

import gurobi.GRBVar;

public class MetricVector {
	
	private HashMap<Router_SDN_Border, Long> weights;
	private HashMap<Router_OSPF, Router_SDN_Border> forwarding;
	private Network_OSPF subdomain;
	private HashMap<Router, GRBVar> isUsedFor;
	
	MetricVector(Network_OSPF subdomain, HashMap<Router_SDN_Border, Long> weights){
		this.subdomain = subdomain;
		this.weights = weights;
		this.forwarding = new HashMap<Router_OSPF, Router_SDN_Border>();
		this.determineForwarding();
		this.isUsedFor = new HashMap<Router, GRBVar>();
	}
	
	public GRBVar get_GUROBI_Variable(Router destination){
		return this.isUsedFor.get(destination);
	}
	
	public void assign_GUROBI_Variable(Router destination, GRBVar variable){
		this.isUsedFor.put(destination, variable);
	}
	
	public boolean allows(MetaPath mp){
		if (!mp.requires_MetricVector_in(subdomain))
			return true;
		for (PathElement pE : mp.getPathelements())
			// the PathElement must be an IP_Path
			if (pE.isPath())
				// the source of that IP_Path must be in my sub-domain
				if (this.subdomain.getRouters().contains(pE.getSource()))
					// the destination of the MetaPath must be external of this sub-domain
					if (!this.subdomain.getRouters().contains(mp.getDestination()))
						// return whether this MetricVector allows for the use of that border node
						return this.forwarding.get(pE.getPath().getSource()).equals(pE.getDestination());
		return false;
	}
	
	@Override
	public boolean equals(Object other){
	    if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof MetricVector)) return false;
	    MetricVector otherSolutionSet = (MetricVector) other;
	    if (otherSolutionSet.weights.keySet().size() != this.weights.keySet().size()) return false;
	    for (Router_SDN_Border borNode : this.subdomain.getBorderNodes())
	    	if (this.getWeight(borNode) != otherSolutionSet.getWeight(borNode))
	    		return false;
	    return true;
	}
	
    @Override
    public int hashCode() {
        return this.weights.hashCode();
    }
	
    public Integer compare(MetricVector other){
    	if (other==null)
    		return this.weights.keySet().size();
    	int difference = 0;
    	for (Router_SDN_Border ownNode : this.subdomain.getBorderNodes())
        	for (Router_SDN_Border foreignNode : other.subdomain.getBorderNodes())
        		if (ownNode.sameNode(foreignNode))
        			if (this.weights.get(ownNode) != other.weights.get(foreignNode))
        				difference++;
    	return difference;
    }
    
    boolean contains_metric_for(Router_SDN_Border bordernode){
    	return this.weights.keySet().contains(bordernode);
    }
    
	public Long getWeight(Router_SDN_Border borderNode){
		return this.weights.get(borderNode);
	}

	public boolean sameRouting(MetricVector other_solution){
		for (Router_OSPF regNode : this.subdomain.getRegularNodes())
			if (!this.getBorderNodeFor(regNode).equals(other_solution.getBorderNodeFor(regNode)))
				return false;
		return true;
	}
	
	public int lsa_distance(MetricVector other_solution){
		int cost = 0;
		for (Router_SDN_Border borNode : this.subdomain.getBorderNodes())
			if (this.weights.get(borNode) != other_solution.weights.get(borNode))
				cost++;
		return cost;
	}
	
	public Router_SDN_Border getBorderNodeFor(Router_OSPF regNode){
		Router_SDN_Border bn = forwarding.get(regNode);
		if (bn==null)
			throw new Error("MetricVector.getBorderNodeFor(Router_OSPF " + regNode.getName() + ") == null");
		return bn;
	}
	
	public LinkedList<Router_OSPF> nodes_that_forward_to(Router_SDN_Border bordNode){
		LinkedList<Router_OSPF> theList = new LinkedList<Router_OSPF>();
		for (Router_OSPF regNode : this.subdomain.getRegularNodes())
			if (forwarding.get(regNode).equals(bordNode))
				theList.add(regNode);
		return theList;
	}
	
	public void determineForwarding(){
		for (Router_OSPF regNode : this.subdomain.getRegularNodes()){
			if (regNode instanceof Router_SDN_Border)
				continue;
			long minCost = 10000000000000L;
			Router_SDN_Border exit_for_this_rn = null;
			for (Router_SDN_Border borNode : this.subdomain.getBorderNodes()){
				long dst = (long) regNode.getPath(borNode).getPathMetric();
				long wgt = weights.get(borNode);
				if (minCost > dst + wgt){
					minCost = dst + wgt;
					exit_for_this_rn = borNode;
				}
			}
			if (exit_for_this_rn == null)
				throw new Error("ERROR: There is no exit border node for the regular OSPF node " + regNode.getName());
			forwarding.put(regNode, exit_for_this_rn);
		}
	}
	
	@Override
	public String toString(){
		String out = "Link Weights:";
		for (Router_SDN_Border bn : this.subdomain.getBorderNodes())
			out += " " + this.weights.get(bn);
		return out;
	}
	
	public String exitVector(){
		String out = "[";
		for (int i=0; i<this.subdomain.getBorderNodes().size()-1; i++)
			out += this.nodes_that_forward_to(this.subdomain.getBorderNodes().get(i)).size() + ",";
		out += this.nodes_that_forward_to(this.subdomain.getBorderNodes().getLast()).size() + "]";
		return out;
	}
	
	@Override
	public MetricVector clone(){
		HashMap<Router_SDN_Border, Long> cloneweights = new HashMap<Router_SDN_Border, Long>();
		for (Router_SDN_Border bn : this.subdomain.getBorderNodes())
			cloneweights.put(bn, 0 + this.weights.get(bn));
		return new MetricVector(this.subdomain, cloneweights);
	}

	
	public MetricVector convert_to(Network_OSPF newSubdomain) {
		HashMap<Router_SDN_Border, Long> oldWeights = this.weights;
		HashMap<Router_SDN_Border, Long> newWeights = new HashMap<Router_SDN_Border, Long>();
		for (Router_SDN_Border foreignBordernode : oldWeights.keySet())
			for (Router_SDN_Border ownBordernode : this.subdomain.getBorderNodes())
				if (foreignBordernode.sameNode(ownBordernode))
					newWeights.put(ownBordernode, oldWeights.get(foreignBordernode));
		return new MetricVector(newSubdomain, newWeights);
	}
	
}
