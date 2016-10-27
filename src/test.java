import java.io.File;

public class test {

	public static void main(String[] args) {
		File folder = new File(".\\results");
		File[] files = folder.listFiles();
		for (File f : files)
			System.out.println(f.getName());
	}

}
