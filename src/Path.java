import java.util.LinkedList;

import gurobi.GRBVar;

public interface Path {
	
	/**
	 * Returns the number of nodes on this path.
	 * 
	 * @return			number of nodes
	 */
	public Integer length();
	
	/**
	 * Returns the position of the node on this path.
	 * I.e., the source returns 0 and the destination returns size()-1.
	 * If this path doesn't contain the node, it returns -1.
	 * 
	 * @param	router	the node whose position on this path is checked
	 * @return			the distance of the node from the source along this path,
	 * 					and -1 if the node is not on this path
	 */
	public int getHopIndex(Router router);
	
	/**
	 * Returns the list of links that connect the nodes of this path
	 * 
	 * @return			list of links
	 */
	public LinkedList<Link> getLinks();
	
	/**
	 * Returns the node of this path the position of the parameter.
	 * I.e., the source returns 0 and the destination returns size()-1.
	 * If this path doesn't contain the node, it returns -1.
	 * 
	 * @param	index	the position of the node on this path
	 * @return			the node at the position of the parameter
	 */
	public Router getNode(int index);
	
	/**
	 * Returns the source node of this path.
	 * 
	 * @return			the source node of this path
	 */
	public Router getSource();
	
	/**
	 * Returns the destination node of this path.
	 * 
	 * @return			the destination node of this path
	 */
	public Router getDestination();
	
	/**
	 * Returns the list of nodes that represents this path.
	 * 
	 * @return			list of nodes that represent this path
	 */
	public LinkedList<Router> getNodes();
	
	/**
	 * Allows the GUROBI optimizer object to access its path variable that is associated with this path.
	 * 
	 * @return	The GUROBI variable that represents the usage of this path.
	 */
	public GRBVar get_GUROBI_Variable();
	
	/**
	 * Allows the GUROBI optimizer object to associate its path variable with this path.
	 * 
	 * @param variable		The GUROBI variable that represents the usage of this path.
	 */
	public void assign_GUROBI_Variable(GRBVar variable);

}
