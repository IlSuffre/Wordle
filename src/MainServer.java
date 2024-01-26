//to compile and run 
//dentro src
//javac -cp "/Users/suffre/Unipi/Lab3/Progetto Finale /Wordle/libs /gson-2.10.jar" ./*.java -d ./../bin
//mi sposto in bin ----- validi  solo per macos
//java -cp ".:./../libs/gson-2.10.jar" MainClient     
//java -cp ".:./../libs/gson-2.10.jar" MainServer

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.reflect.*;
import java.lang.reflect.*;


public class MainServer {
    public static int port;
    public static int timer_word;
    public static String host;
    public static int port_2;
    public static final String configFile = "server.properties";

    public static void main (String[] args) throws IOException{
        readConfig();
        ArrayList<User> users_list = new ArrayList<User>();
        File users_file = new File("users.json");
        // Se file users.json esiste carico le sue informazioni (ovvero gli utenti) in un array list
        if (users_file.exists()){
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(users_file));
            Type list_user_type = new TypeToken<ArrayList<User>>() {}.getType();
            users_list = gson.fromJson(reader,list_user_type);
        }
        else{ //altrmineti creo file json vuoto
            users_file.createNewFile();
        }
        //creo arraylist con tutte le parole 
        File f = new File("words.txt");
        ArrayList<String> dictionary = create_dictionary(f);
        //creo oggetto chiave Info_Server con secret_word nulla
        Info_Server info = new Info_Server(null, dictionary,users_list);
        Timer timer = new Timer();
        long delay = 0;
        long period = timer_word;
        //creo thread ciclico temporizzato che estrae parole da info.dictionary e la salva in info.secret_word
        timer.scheduleAtFixedRate(new Change_Word(info), delay, period);
        //creo thread pool per gestire connessioni con client multipli
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(port)){
            while(true){  
                Socket socket = server.accept();
                System.out.println("Client connected");
                //per ogni client connesso crea un WordleTask che si occupa delle sue richieste
                pool.execute(new WordleTask(socket,info,host,port_2)); 
                // Tutta la comunicazione è gestita dalla classe WordleTask
            }
    
        }
        catch(IOException e) {System.out.println("Server Socket opening error");}
    }

    //Prende un file e di parole, lo scorre e salva le parole in un arraylist
    public static ArrayList<String> create_dictionary(File f) throws FileNotFoundException{
        Scanner s = new Scanner(f);
        ArrayList<String> dictionary = new ArrayList<String>();
        while (s.hasNext()){
            dictionary.add(s.next());
        }
        s.close();
        return dictionary;
    }

    //Legge parametri da file di configurazione e li salva nelle variabili appositamente istanziate
    public static void readConfig() throws FileNotFoundException, IOException {
		InputStream input = new FileInputStream(configFile);
        Properties prop = new Properties();
        prop.load(input);
        port = Integer.parseInt(prop.getProperty("port"));
        timer_word = Integer.parseInt(prop.getProperty("timer"));
        host = prop.getProperty("host");
        port_2 = Integer.parseInt(prop.getProperty("port_2"));
        input.close();
	}

}
