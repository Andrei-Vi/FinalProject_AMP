# FinalProject AMP - Local Chat Application

Aplicatie Java de chat local, construita cu arhitectura client-server. Serverul ruleaza pe `localhost`, iar fiecare client pornit separat se poate conecta, isi poate crea cont, poate intra intr-o camera si poate trimite mesaje sau fisiere.

## Motivatie

Am ales sa construiesc o aplicatie de chat deoarece proiectul combina mai multe concepte importante din cerintele de la clasa intr-un exemplu practic si usor de testat. Scopul lui a fost sa inteleg mai bine cum se leaga intre ele clasele de model, serviciile, persistenta, exceptiile si comunicarea prin retea intr-o aplicatie reala.

Prin acest proiect am exersat:

- organizarea codului pe pachete clare: `model`, `service`, `repository`, `server`, `client`, `network`, `exception`
- programarea orientata pe obiecte, folosind clase pentru useri, camere, sesiuni, mesaje si transferuri de fisiere
- arhitectura client-server si comunicarea prin socket-uri pe `localhost`
- persistenta datelor in fisiere CSV si separarea logicii de citire/scriere in repository-uri
- tratarea erorilor prin exceptii custom, cu mesaje clare pentru utilizator
- folosirea rolurilor, prin separarea intre user regular si admin
- actiuni administrative precum ban, unban, audit logs si stergere audit
- validarea inputului pentru username, parola, mesaje si fisiere
- masuri simple de securitate, precum mesaj generic la login esuat si blocare temporara dupa incercari repetate gresite
- testarea manuala a fluxurilor principale: creare cont, login, camere, mesaje, fisiere si actiuni de admin

Aplicatia m-a ajutat sa vad diferenta dintre un cod care doar ruleaza si un cod care este mai bine structurat, mai usor de extins si mai sigur pentru utilizator.

## Cerinte

- JDK 25
- IntelliJ IDEA sau un terminal cu Java configurat

In IntelliJ, seteaza `Project SDK` la JDK 25.

## Rulare din IntelliJ

Proiectul include doua run configurations:

- `Run Server`
- `Run Client`

Pasii de rulare:

1. Porneste `Run Server`.
2. Porneste `Run Client` pentru primul utilizator.
3. Porneste din nou `Run Client` pentru al doilea utilizator.
4. Fiecare client este o instanta separata si se conecteaza la acelasi server: `127.0.0.1:5000`.

Nu trebuie schimbat IP-ul pentru fiecare client. Diferenta dintre clienti este facuta automat prin conexiuni socket separate.

## Rulare din terminal

Compileaza proiectul:

```powershell
javac -d out $(Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

Porneste serverul:

```powershell
java -cp out server.ChatServer 5000 data
```

Porneste un client:

```powershell
java -cp out client.ChatClient 127.0.0.1 5000
```

Porneste comanda de client de mai multe ori pentru mai multi utilizatori.

## Conturi demo

- `admin / admin123`
- `ana / ana123`
- `mihai / mihai123`

Se pot crea si conturi noi din meniul clientului.

## Functionalitati

- creare cont
- login/logout
- camere de chat
- mesaje trimise prin socket client-server
- afisare mesaje in timp real pentru utilizatorii din aceeasi camera
- trimitere fisier prin path local
- descarcare fisier prin copiere intr-un folder ales
- tag pentru fisiere: `SENT` / `DOWNLOADED`
- validare username si parola la creare cont
- mesaj generic pentru login esuat
- blocare temporara dupa prea multe incercari gresite de login
- actiuni admin: ban user, unban user, audit logs si stergere audit logs
- persistenta in fisiere CSV din folderul `data`

## Structura proiectului

- `client.ChatClient` - aplicatia de client
- `server.ChatServer` - serverul aplicatiei
- `service` - logica principala a aplicatiei
- `model` - clasele domeniului
- `repository` - citire/scriere CSV
- `network` - protocolul de comunicare prin socket
- `exception` - exceptii custom
- `data` - fisiere CSV pentru datele aplicatiei
