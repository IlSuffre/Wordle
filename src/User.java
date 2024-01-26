import java.util.HashMap;

//Usato solo come oggetto statico
//Contiene solo informazioni che secondo me sono necessarie e utili da memorizzare per ogni utente 
//i nomi delle variabili dovrebbero essere autoesplicativi e non necessitare di commento
public class User {
    String username;
    String password;
    boolean play_this_word;
    int games_played;
    int games_won;
    int victory_rate;
    int victory_streak;
    int max_victory_streak;
    HashMap<Integer,Integer> guess_distribution;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.play_this_word = false;
        this.games_played = 0;
        this.games_won = 0;
        this.victory_rate = 0;
        this.victory_streak = 0;
        this.max_victory_streak = 0;
        HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
        map.put(1,0);
        map.put(2,0);
        map.put(3,0);
        map.put(4,0);
        map.put(5,0);
        map.put(6,0);
        map.put(7,0);
        map.put(8,0);
        map.put(9,0);
        map.put(10,0);
        map.put(11,0);
        map.put(12,0);
        //questa mappa presenta come chiavi il numero di tentativi effettuati per indovinare una parola
        //e come valore il numero di parole indovinate con quei tentativi
        this.guess_distribution = map; 

    }
}
