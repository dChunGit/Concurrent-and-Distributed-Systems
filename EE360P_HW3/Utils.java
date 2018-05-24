import java.io.PrintWriter;

public class Utils {
    static private int recordId = 1;

    synchronized public static String processRequest(String request) {
        String[] requestArray = request.split(":");
        String op = requestArray[0];


        switch(op) {
            //borrow a book
            case "b": {
                String student = requestArray[1];
                String book = requestArray[2];

                int bookInventory = BookServer.inventory.get(book);
                if(bookInventory > 0) {
                    //take book from inventory and add to checked list
                    BookServer.inventory.put(book, bookInventory - 1);
                    String[] bookData = {book, student};
                    BookServer.checkedRecords.put(recordId, bookData);

                    String result = "You request has been approved, " + recordId + " " + student + " " + book;
                    recordId++;
                    return result;
                } else return "Request Failed - Book not available";
            }
            //return a book
            case "r": {
                int recordReq = Integer.valueOf(requestArray[1]);

                //if we have a recordId checked out
                if(BookServer.checkedRecords.containsKey(recordReq)) {
                    //get the data, remove it from checked and put the book back into inventory
                    String book = BookServer.checkedRecords.get(recordReq)[0];
                    BookServer.checkedRecords.remove(recordReq);
                    BookServer.inventory.put(book, BookServer.inventory.get(book) + 1);

                    return recordReq + " is returned";
                } else {
                    return recordReq + " not found, no such borrow record";
                }
            }
            //inventory
            case "i": {
                StringBuilder sb = new StringBuilder();
                for(String book : BookServer.bookOrder) {
                    sb.append(book + " " + BookServer.inventory.get(book) + ":");
                }
                return sb.toString();

            }
            //list borrowed books
            case "l": {
                String student = requestArray[1];
                StringBuilder sb = new StringBuilder();

                //get all the borrowed books for a student and append it
                for(int recId : BookServer.checkedRecords.keySet()) {
                    String[] data = BookServer.checkedRecords.get(recId);
                    if(data[1].equalsIgnoreCase(student)) {
                        sb.append(recId + " " + data[0] + "\n");
                    }
                }
                String response = sb.toString();
                return response.substring(0, response.length() - 2);

            }
            //exit server
            case "exit": {
                //print inventory
                try {
                    PrintWriter writer = new PrintWriter("inventory.txt", "UTF-8");
                    for (String book : BookServer.inventory.keySet()) {
                        writer.println(book + " " + BookServer.inventory.get(book));
                    }

                    writer.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } break;
            default: return "ERROR";
        }

        return "ERROR";
    }
}
