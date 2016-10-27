import java.util.LinkedList;

public interface MultiPath {
	
	/**
	 * Returns the number of paths which are contained in this object.
	 * @return	The number of paths.
	 */
	public int size();
	
	public int getK();

	public void setK(int k);
	
	public Router getSource();

	public Router getDestination();

	public Path getPath(int index);
	
	public LinkedList<Path> getPaths();
	
	public boolean add(Path newPath);
	
	public boolean isFull();
	
	public int length_of_longest_path();
	
}
