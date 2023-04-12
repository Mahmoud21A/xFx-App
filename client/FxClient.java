import java.net.*;
import java.io.*;
import java.util.StringTokenizer;
import java.nio.channels.FileChannel;

public class FxClient {
	public static void main(String[] args) throws Exception {
		String command = args[0];
		String fileName = args[1];

		try (Socket connectionToServer = new Socket("localhost", 80)) {

			// I/O operations

			InputStream in = connectionToServer.getInputStream();
			OutputStream out = connectionToServer.getOutputStream();

			BufferedReader headerReader = new BufferedReader(new InputStreamReader(in));
			BufferedWriter headerWriter = new BufferedWriter(new OutputStreamWriter(out));

			DataInputStream dataIn = new DataInputStream(in);
			DataOutputStream dataOut = new DataOutputStream(out);

			if (command.equals("d")) {
				long startTime = System.currentTimeMillis();
				String header = "download " + fileName + "\n";
				headerWriter.write(header, 0, header.length());
				headerWriter.flush();

				header = headerReader.readLine();

				if (header == null || header.equals("NOT FOUND")) {
					System.out.println("We're extremely sorry, the file you specified is not available!");
				} else {
					String[] tokens = header.split(" ");

					String status = tokens[0];

					if (status.equals("OK")) {

						// Checks if the file is already cached
						File cachedFile = new File("ClientCache/" + fileName);
						long lastModifiedTimeCache = cachedFile.lastModified();
						// System.out.println("lastModifiedTimeCache: " + lastModifiedTimeCache);

						long lastModifiedTimeServer = Long.parseLong(tokens[2]);
						// System.out.println("lastModifiedTimeServer:" + lastModifiedTimeServer);

						boolean cacheExists = cachedFile.exists();

						if (cacheExists && lastModifiedTimeServer <= lastModifiedTimeCache) {
							int fileSize = Integer.parseInt(tokens[1].trim());
							int fileSizeCache = (int) cachedFile.length();

							if (fileSize == fileSizeCache) {
								System.out.println("Unchanged file already in cache. No need to download.");
								try (FileInputStream fileIn = new FileInputStream(cachedFile);
										FileChannel source = fileIn.getChannel();
										FileOutputStream fileOut = new FileOutputStream("ClientShare/" + fileName);
										FileChannel destination = fileOut.getChannel()) {
									destination.transferFrom(source, 0, source.size());
								} catch (IOException e) {
									System.out.println("Error copying file from cache: " + e.getMessage());
								} finally {
									String finHeader = "FIN\n";
									headerWriter.write(finHeader, 0, finHeader.length());
									headerWriter.flush();
								}
							} else {
								System.out.println("File in cache is outdated. Downloading from server.");
								String header2 = "download " + fileName + "\n";
								headerWriter.write(header2, 0, header2.length());
								headerWriter.flush();

								int chunkSize = Math.min(fileSize, 1024);
								byte[] space = new byte[chunkSize];

								long lastDownloadedByte = cachedFile.length();

								try {
									// long n =
									dataIn.skip(lastDownloadedByte);
									// System.out.println("SKIP: " + n);

									try (FileOutputStream fileOut = new FileOutputStream("ClientShare/" + fileName,
											true);
											FileOutputStream cacheOut = new FileOutputStream(cachedFile, true)) {

										int bytesRead = 0;
										int totalBytesRead = (int) lastDownloadedByte;
										while ((bytesRead = dataIn.read(space)) > 0) {
											cacheOut.write(space, 0, bytesRead);
											fileOut.write(space, 0, bytesRead);
											totalBytesRead += bytesRead;
											if (totalBytesRead >= fileSize) {
												break;
											}
										}
									}
								} catch (EOFException e) {
									System.out.println("Error reading file data from server: " + e.getMessage());
								} catch (IOException e) {
									System.out.println("Error writing file to share or cache: " + e.getMessage());
								} finally {
									String finHeader = "FIN\n";
									headerWriter.write(finHeader, 0, finHeader.length());
									headerWriter.flush();
								}
							}
						} else {
							System.out.println("File not in cache. Downloading from server.");
							int fileSize = Integer.parseInt(tokens[1].trim());

							String header2 = "download " + fileName + "\n";
							headerWriter.write(header2, 0, header2.length());
							headerWriter.flush();

							int chunkSize = Math.min(fileSize, 1024);
							byte[] space = new byte[chunkSize];

							try (FileOutputStream fileOut = new FileOutputStream("ClientShare/" + fileName);
									FileOutputStream cacheOut = new FileOutputStream(cachedFile)) {
								int bytesRead = 0;
								int totalBytesRead = 0;
								while ((bytesRead = dataIn.read(space)) > 0) {
									cacheOut.write(space, 0, bytesRead);
									fileOut.write(space, 0, bytesRead);
									// REMOVE THIS
									// if (totalBytesRead == 1024 * 5) // Will write 6144 bytes
									// Runtime.getRuntime().exit(0);
									totalBytesRead += bytesRead;
									if (totalBytesRead >= fileSize) {
										break;
									}
								}

							} catch (EOFException e) {
								System.out.println("Error reading file data from server: " + e.getMessage());
							} catch (IOException e) {
								System.out.println("Error writing file to share or cache: " + e.getMessage());
							} finally {
								String finHeader = "FIN\n";
								headerWriter.write(finHeader, 0, finHeader.length());
								headerWriter.flush();
							}
						}
					} else {
						System.out.println("You're not connected to the right Server!");
					}
				}
				long endTime = System.currentTimeMillis();
				long timeTaken = endTime - startTime;
				System.out.println("Time taken to download: " + timeTaken + "ms");

			} else if (command.equals("u")) {
				try {
					FileInputStream fileIn = new FileInputStream("ClientShare/" + fileName);
					int fileSize = fileIn.available();

					String header = "upload" + " " + fileName + " " + fileSize + "\n";

					headerWriter.write(header, 0, header.length());
					headerWriter.flush();

					byte[] bytes = new byte[fileSize];
					fileIn.read(bytes);

					fileIn.close();

					dataOut.write(bytes, 0, fileSize);

				} catch (Exception ex) {
					String errorMessage = "FILE DOES NOT EXIST";
					headerWriter.write(errorMessage, 0, errorMessage.length());
					headerWriter.flush();
				}
			} else if (command.equals("l")) {
				String header = "list " + fileName + "\n";
				headerWriter.write(header, 0, header.length());
				headerWriter.flush();

				String files = headerReader.readLine();
				while (files != null) {
					System.out.println(files + "\n");
					files = headerReader.readLine();
				}

			} else {
				System.out.println("Invalid command.");
			}
		}
	}
}
