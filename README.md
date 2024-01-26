# Wordle

Versione rivisitata del celebre gioco del New York Times sviluppata in java con interfaccia a riga di comando (CLI)

Il gioco (client-server) presenta un meccanismo di autenticazione per utente in modo da salvare tutti i progressi e le statisctiche personali.
Dati personali salvati in un file Json che viene modificato all'occorrenza tramite la libreria GSON.

Principali differenze dalla versione originale :
- parole lunghe 10 lettere
- 12 tentativi disponibili per indovinare
- codifica colori-> lettere indovinate (verdi) +, lettere presenti ma in posizione sbagliata (gialle) ?, lettere non presenti nella parola (grigie) X
- possibilità di condividere il proprio risultato dell'ultima parola giocata con tutti gli utenti online tramite multicast
