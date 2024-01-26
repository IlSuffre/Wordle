import java.util.ArrayList;

//Oggetto statico usato per passare a tutti i thread la secret_word il dictionary e l'users list 
public class Info_Server {
    String  secret_word;
    ArrayList<String> dictionary;
    ArrayList<User> users_list;

    public Info_Server(String secret_word, ArrayList<String> dictionary,ArrayList<User> users_list){
        this.secret_word=secret_word;
        this.dictionary=dictionary;
        this.users_list=users_list;
    }

}