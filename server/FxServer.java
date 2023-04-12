import java.net.*;
import java.io.*;
import java.util.StringTokenizer;

public class FxServer {

	public static void main(String[] args) throws Exception {

		try (ServerSocket ss = new ServerSocket(80)) {
			while (true) {
				System.out.println("Server waiting...");
				Socket connectionFromClient = ss.accept();
				System.out.println(
						"Server got a connection from a client whose port is: " + connectionFromClient.getPort());

				try {
					InputStream in = connectionFromClient.getInputStream();
					OutputStream out = connectionFromClient.getOutputStream();

					String errorMessage = "NOT FOUND\n";

					BufferedReader headerReader = new BufferedReader(new InputStreamReader(in));
					BufferedWriter headerWriter = new BufferedWriter(new OutputStreamWriter(out));

					DataInputStream dataIn = new DataInputStream(in);
					DataOutputStream dataOut = new DataOutputStream(out);

					String header = headerReader.readLine();
					StringTokenizer strk = new StringTokenizer(header, " ");

					String command = strk.nextToken();

					String fileName = strk.nextToken();

					if (command.equals("download")) {
						try {
							File file = new File("ServerShare/" + fileName);
							FileInputStream fileIn = new FileInputStream(file);

							int fileSize = fileIn.available();
							long lastModifiedTimeServer = file.lastModified();
							header = "OK " + fileSize + " " + lastModifiedTimeServer + "\n";

							headerWriter.write(header, 0, header.length());
							headerWriter.flush();

							byte[] bytes = new byte[fileSize];
							fileIn.read(bytes);

							String finHeader = headerReader.readLine();
							if (finHeader.equals("FIN")) {
								fileIn.close();
							}

							dataOut.write(bytes, 0, fileSize);
							dataOut.flush();

						} catch (FileNotFoundException ex) {
							headerWriter.write(errorMessage, 0, errorMessage.length());
							headerWriter.flush();
						} catch (IOException ex) {
							headerWriter.write("ERROR " + ex.getMessage(), 0, ex.getMessage().length());
							headerWriter.flush();
						} finally {
							connectionFromClient.close();
						}

					} else if (command.equals("upload")) {

						String[] tokens = header.split(" ");

						String fileName2 = tokens[1];
						int size = Integer.parseInt(tokens[2].trim());

						byte[] space = new byte[size];

						dataIn.readFully(space);

						try (FileOutputStream fileOut = new FileOutputStream("ServerShare/" + fileName2)) {
							fileOut.write(space, 0, size);

						} catch (Exception ex) {
							headerWriter.write(errorMessage, 0, errorMessage.length());
							headerWriter.flush();

						} finally {
							connectionFromClient.close();
						}

					} else if (command.equals("list")) {
						File folder = new File("ServerShare/");

						File[] listOfFiles = folder.listFiles();

						StringBuilder fileList = new StringBuilder();

						for (File file : listOfFiles) {
							fileList.append(file.getName() + "\n");
						}

						header = fileList.toString();

						headerWriter.write(header, 0, header.length());
						headerWriter.flush();

						connectionFromClient.close();

					} else {

						System.out.println("Connection got from an incompatible client");

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
