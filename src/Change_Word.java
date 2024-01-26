import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.TimerTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class Change_Word extends TimerTask{

    Info_Server info;
    public Change_Word(Info_Server info){
        this.info = info;
    }

    public void run() {
        //cambia la parola da indovinare
        info.secret_word = extract_word(info.dictionary);
        //aggiornare variabile booleana play_this_word per permettere agli utenti di giocare la nuova parola
        now_can_play_again(info.users_list);
        try {
            //backup lista utenti con variabile per giocare aggiornata
            backup_server_users(info.users_list);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Usato per visualizzare la secret_word sul server per test in quanto troppo difficile indovinare
        System.out.println("New secret word : " + info.secret_word);
    }

    //estrae una parola a caso dal dictionary
    //trasformo arraylist in array ed estraggo un indice a caso dal quale prendere una parola
    public String extract_word(ArrayList<String> dictionary){
        String[] diz = dictionary.toArray(new String[0]);
        Random random = new Random();
        int i = random.nextInt(diz.length);
        return diz[i];
    }

    //Modifica la variabile play_this_word di tutti gli utenti in modo da permettergli di giocare la nuova parola
    public void now_can_play_again(ArrayList<User> users_list){
        Iterator<User> iter = users_list.iterator();
        while(iter.hasNext()){
            User user = iter.next();
            user.play_this_word = false;
        }
    }

    //Sovrascrive il file users.json con l'users_list aggiornata 
    public synchronized void backup_server_users(ArrayList<User> users_list) throws IOException{
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File users_file = new File("users.json");
        FileWriter fw = new FileWriter(users_file);
        String s_json = gson.toJson(users_list);
        fw.write(s_json);
        fw.close();
    }


    
}
