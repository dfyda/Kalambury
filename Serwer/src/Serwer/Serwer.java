package Serwer;

/** Kalambury - Serwer
* Projekt do kursu <b>Programowanie w Sieci Internet</b> 
* @author Damian Fyda
* @author dfyda@wsb-nlu.edu.pl 
* @version 1.2 lipiec 2014 
*/
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.border.*;

/** Klasa steruje rozgrywką
     */ 
class PrzebiegGry
{
    /** Zmienna haslo typu String służąca do przechowywania aktualnie zgadywanego hasła
     */ 
    static String haslo;
    /** Zmienna rusyjeTeraz typu OpisGracza służąca do przechowywania dane rysujacego gracza
     */ 
    static OpisGracza rusyjeTeraz;
    /** Zmienna maxLiczbaPunktow typu int służąca do przechowywania maksymalnej ilosci punktów po osiągnięciu których rozgrywka się kończy
     */ 
    static int maxLiczbaPunktow = 300;
    /** Zmienna ktoRysuje typu LinkedList<OpisGracza>. Kolejka służąca do przechowywania graczy na potrzeby wyboru rysujacego
     */ 
    static LinkedList<OpisGracza> ktoRysuje = new LinkedList<OpisGracza>();
    /** Zmienna czyRysuje typu boolean słuzy do zabezpieczenia się przed rysowaniem przez kilku graczy jednoczesnie
     */ 
    static boolean czyRysuje = false;//zabezpieczenie przed rysowaniem przez nowego gracza
    static int punktyZaZgadniecie = 50;
    static int punktyZaNarysowanie = 20;
    /** Zmienna nrOstatniegoGracza typu int słuzy do zapamiętywania ostatniego numeru gracza, potrzebna do poprawnego numerowania graczy
    */ 
    static int nrOstatniegoGracza = 0;
    /** 
    @return Metoda zwraca wylosowane haslo
    */
    static String losujHaslo()
    {
        Random generator = new Random();
        return Serwer.haslaDoZgadywania.get(generator.nextInt(Serwer.haslaDoZgadywania.size()));
    }  
    /** 
    Metoda służąca do rozpoczecia rozgrywki, jeśli jest przynajmniej dwóch graczy, wybieramy pierwszego gracza z kolejki, losujemy dla niego hasło i rozpoczynamy rozgrywkę
    */
    static void zacznijGre()
    {
        if(ktoRysuje.size() == 0) //Gdy kolejka pusta, ustaw odpowiednia flage
            czyRysuje = false;
        if(PrzebiegGry.ktoRysuje.size()>1 && czyRysuje==false)
                {
                   WatekGracza.powiedzWszystkim("<<GRA>>");
                   PrzebiegGry.haslo=PrzebiegGry.losujHaslo();
                   OpisGracza rysujacy = PrzebiegGry.ktoRysuje.poll();//wyciagamy z kolejki
                   rusyjeTeraz = rysujacy;
                   PrzebiegGry.ktoRysuje.offer(rysujacy);//wstawiamy na koniec kolejki
                   rysujacy.pisz("<<HASLO>>" + PrzebiegGry.haslo);
                   czyRysuje = true;
                }
    }
    /** 
    Metoda sprawdza czy jakiś gracz uzyskał odpowiednią liczbę punktów, aby zakonczyc rozgrywke 
    @return Metoda zwraca nazwe gracza, który uzyskal odpowiednia liczbe punktow
    */
    static String sprawdzPunkty()
    {
        String wygrany = "";
        Iterator iterator = WatekGracza.gracze.iterator();
        OpisGracza nastepny;
        while(iterator.hasNext())
        {
            nastepny=(OpisGracza)iterator.next();
            if (nastepny.punkty >= maxLiczbaPunktow)
            {
                wygrany = nastepny.nazwa;
                zerujPunkty();
                break;
            }
        }
        return wygrany;
    }
    /** 
    Metoda zeruje kazdemu graczowi liczbe zdobytych punktow
    */
    static void zerujPunkty()
    {
        Iterator iterator = WatekGracza.gracze.iterator();
        OpisGracza nastepny;
        while(iterator.hasNext())
        {
            nastepny=(OpisGracza)iterator.next();
            nastepny.punkty = 0;
        }
    }
}
/** Klasa odpowiada za okno ze zmianą haseł
     */ 
class HaslaOkno extends JDialog implements ActionListener//Tworzenie okna ze zmiana hasel
{
    JTextArea hasla;
    JButton zapisz;
    HaslaOkno()
    {
        super();
    }
    void init() 
    {
        setSize(400, 300);

        setLayout(new BorderLayout());
        setResizable(false);
        
        Component bpion = Box.createHorizontalStrut(10);
        Component bpion2 = Box.createHorizontalStrut(10);
        Component bpoziom = Box.createVerticalStrut(10);
        add(bpion, BorderLayout.EAST);
        add(bpion2, BorderLayout.WEST);
        add(bpoziom, BorderLayout.NORTH);
        
        JPanel panelSrodek = new JPanel();
        panelSrodek.setLayout(new BorderLayout());
        add(panelSrodek, BorderLayout.CENTER);
        
        hasla = new JTextArea();
        hasla.setBorder(new TitledBorder("Hasła(każde w osobnej linii): "));
        panelSrodek.add(hasla,BorderLayout.CENTER);
        panelSrodek.add(new JScrollPane(hasla));
        
        zapisz = new JButton("Zapisz");
        panelSrodek.add(zapisz,BorderLayout.SOUTH);
        zapisz.addActionListener(this);
        
        czytajPlik();
    }
    public void actionPerformed(ActionEvent zdarzenie)
    {
        Object zrodlo = zdarzenie.getSource();
        if (zrodlo == zapisz)
        {
            zapiszPlik();
        }
    }
    /** 
    Metoda wypisuje z pliku dostępne hasła do tablicy haseł
    @throws Exception z informacją o nieudanej próbie czytania z pliku
    */
    void czytajPlik()
    {    
      try 
        {
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(Serwer.filename)));
            hasla.setText("");
            String linia;
            while(true) {                
                linia = in.readLine();
                if(linia == null) break;
                
                hasla.append(linia + "\n");                
            }
            in.close(); 
        } 
        catch(Exception e) 
        { 
            System.out.println(e); 
        }                
    }
    /** 
    Metoda zapisuje do pliku obecne hasła
    @throws Exception z informacją o nieudanej próbie zapisu do pliku
    */
    void zapiszPlik()
    {
    try 
        {
            BufferedWriter out = new BufferedWriter(
                                       new OutputStreamWriter(
                                           new FileOutputStream(Serwer.filename)));

            out.write(hasla.getText());
            out.close();

        } 
        catch(Exception e) 
        {
            System.out.println(e); 
        }   
        Serwer.wczytajHasla();
    }
}
/** Klasa przechowuje informacje o graczu
     */ 
class OpisGracza 
{
    private PrintWriter wyjscie;
    public String nazwa;
    public int punkty;
    OpisGracza(String nazwa, PrintWriter wyjscie)
    {
        this.nazwa = nazwa;
        this.wyjscie = wyjscie;
        
        punkty = 0;
    }
    /** 
    Metoda wysyla wiadomosc do gracza, z uwzględnieniem nadawcy
    */
    synchronized void piszOdpowiedz(String nadawca, String wiadomosc)
    {
        wyjscie.println("<<WIADOMOSC>>"+nadawca + ": " + wiadomosc);
    }
    /** 
    Metoda wysyla wiadomosc do gracza
    */
    synchronized void pisz(String wiadomosc)
    {
        wyjscie.println(wiadomosc);
    }
    /** 
    Metoda dodaje punkty graczowi
    */
    synchronized void dodajPunkty(int nagroda)
    {
        punkty = punkty + nagroda;
    }
}
/** Klasa wątka gracza
     */ 
class WatekGracza extends Thread
{
    public Socket socket;
    private OpisGracza opis;
    /** Zmienna gracze typu HashSet<OpisGracza>. przechowuje informacje o graczach
    */ 
    static HashSet<OpisGracza> gracze = new HashSet<OpisGracza>();
    private BufferedReader wejscie;
    private PrintWriter wyjscie;
    
    WatekGracza(Socket socket) throws IOException
    {
       this.socket = socket;
       wejscie = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
       wyjscie = new PrintWriter( new BufferedWriter( 
                    new OutputStreamWriter( 
                        socket.getOutputStream())), true); 
       
    }
    /** Metoda usuwa informacje o graczu
    */ 
    private void zakonczDzialanie() throws IOException
    {
        PrzebiegGry.ktoRysuje.remove(opis);
        gracze.remove(opis);
        wejscie.close();
        wyjscie.close();
        socket.close();
    }
    /** Metoda wysyla wiadomosc do wszystych graczy, z uwzględnieniem nadawcy
    */ 
    private void powiedzWszystkimZNadawca(String nazwa, String wiadomosc)
    {
        Iterator iterator = gracze.iterator();
        OpisGracza nastepny;
        while(iterator.hasNext())
        {
            nastepny=(OpisGracza)iterator.next();
            nastepny.piszOdpowiedz(nazwa, wiadomosc);
        }
    }
    /** Metoda wysyla wiadomosc do wszystych graczy bez uwzglednienia nadawcy
    */ 
    static void powiedzWszystkim(String wiadomosc)
    {
        Iterator iterator = gracze.iterator();
        OpisGracza nastepny;
        while(iterator.hasNext())
        {
            nastepny=(OpisGracza)iterator.next();
            nastepny.pisz(wiadomosc);
        }
    }
    /** Metoda wysyla informacje o graczach do wszystkich graczy
    */ 
    private void wypiszGraczy()
    {
        Iterator iterator = gracze.iterator();
        OpisGracza nastepny;
        
        int i = 0;
        while(iterator.hasNext())
        {
            i++;
            nastepny = (OpisGracza)iterator.next();
            if(i==1)
                powiedzWszystkim("<<PIERWSZYGRACZ>><<GRACZ>>"+nastepny.nazwa + " " + nastepny.punkty);
            else
                powiedzWszystkim("<<GRACZ>>"+nastepny.nazwa + " " + nastepny.punkty);
        }
    }
    @Override
    public void run()
    {
        String nazwaGracza=null;
        Date date = new Date();
        try
        {
            nazwaGracza = "Gracz_" + ++PrzebiegGry.nrOstatniegoGracza;
            Serwer.logi.append(date.toString() + " Zalogował się: " + nazwaGracza + "\n");
             // poniższe by widzieć ostatni wpis do JTextArea
            Serwer.logi.setCaretPosition(Serwer.logi.getText().length() - 1);
            Serwer.logi.repaint();
            
            opis = new OpisGracza(nazwaGracza, wyjscie);
            PrzebiegGry.ktoRysuje.offer(opis);
            gracze.add(opis);
            PrzebiegGry.zacznijGre();
            powiedzWszystkimZNadawca("---","Zalogował się: " + nazwaGracza);
            
            wypiszGraczy();
            
            while(true)
            {
                date = new Date();
                String info = wejscie.readLine();
                if(info.contains("<<OBRAZ>>"))//Jesli przesylamy obraz, przekaz go wszystkim
                {
                    powiedzWszystkim(info);
                }
                if(info.contains("<<ODPOWIEDZ>>"))
                {
                   String[] tmp = info.split("<<ODPOWIEDZ>>");//wyczyszczenie z tagu
                   String odpowiedz = tmp[1];
                   powiedzWszystkimZNadawca(nazwaGracza, odpowiedz);
                   if(PrzebiegGry.czyRysuje == true)
                   {
                       odpowiedz = odpowiedz.toLowerCase();
                       if(odpowiedz.equals(PrzebiegGry.haslo.toLowerCase()))//Jesli odpowiedz jest prawidlowa
                       {
                           opis.dodajPunkty(PrzebiegGry.punktyZaZgadniecie);
                           PrzebiegGry.rusyjeTeraz.dodajPunkty(PrzebiegGry.punktyZaNarysowanie);
                           wypiszGraczy();
                           powiedzWszystkim("<<WIADOMOSC>>---- " + nazwaGracza + " ODGADŁ HASŁO!");
                           String wygrany =PrzebiegGry.sprawdzPunkty(); 
                           if(!wygrany.equals(""))//Sprawdzenie czy nie uzyskano maksymalnej liczby punktów
                           {
                               powiedzWszystkim("<<WIADOMOSC>>-------- !!! " + wygrany + " WYGRAŁ GRE !!! --------");
                               wypiszGraczy();
                           }
                           PrzebiegGry.czyRysuje = false;
                           PrzebiegGry.zacznijGre();
                       }
                   }
                }
                if(info.contains("<<<<PODPOWIEDZ>>>>"))//Sprawdza czy podpowiedz, jak tak przesyla wszystkim wiadomosc
                {
                   String[] tmp = info.split("<<<<PODPOWIEDZ>>>>");
                   String odpowiedz = tmp[1];
                   powiedzWszystkim("<<WIADOMOSC>>Podpowiedz: "+ odpowiedz);
                }
                if(info.equals("<<<<REZYGNUJE>>>>"))//Sprawdza czy rezygnacja, jak tak przekazuje rysowanie kolejnemu graczowi
                {
                   powiedzWszystkim("<<WIADOMOSC>>---- " + nazwaGracza + " REZYGNUJE Z RYSOWANIA");
                   PrzebiegGry.czyRysuje = false;
                   PrzebiegGry.zacznijGre();
                }
                if(info.equals("<<<<END>>>>"))//Sprawdza czy zawodnik zakonczyl gre
                {
                   Serwer.logi.append(date.toString() + " Wylogował się: " + nazwaGracza + "\n");
                   // poniższe by widzieć ostatni wpis do JTextArea
                   Serwer.logi.setCaretPosition(Serwer.logi.getText().length() - 1);
                   Serwer.logi.repaint();
            
                   powiedzWszystkimZNadawca("---", "Wylogował się: " + nazwaGracza);
                   
                   zakonczDzialanie();
                   if(PrzebiegGry.rusyjeTeraz.nazwa.equals(nazwaGracza))//Jesli rysowal przekaz rysowanie kolejnemu graczowi
                   {
                       PrzebiegGry.czyRysuje = false;
                       PrzebiegGry.zacznijGre();
                   }
                   wypiszGraczy();
                   break;
                }
            }
        
        }
        catch(Exception e) 
        {
            //Serwer.logi.append("Blad: " + e + " \n");
        }
    }
}
/** Klasa odpowiada za przyjmowanie nowych graczy
     */ 
class WatekUruchomienia extends Thread 
{
    public ServerSocket s = null;
    private Socket socket = null;
    private int port;
    public WatekUruchomienia()
    {
        this.port = Integer.parseInt(Serwer.portSerwera.getText());
    }
    public void run() 
    {
        Date date = new Date();
        try {
            s = new ServerSocket(port);
            Serwer.wlaczSerwer.setEnabled(false);
            Serwer.eStatusSerwera.setText("Serwer włączony");
            Serwer.portSerwera.setEditable(false);
            Serwer.eStatusSerwera.setForeground(new Color(0, 153, 0));
            
            Serwer.logi.append(date.toString() + " Serwer Włączono" + "\n");
            // poniższe by widzieć ostatni wpis do JTextArea
            Serwer.logi.setCaretPosition(Serwer.logi.getText().length() - 1);
            Serwer.logi.repaint();
            
            while (true) {
                //czekamy na klienta
                socket = s.accept();
                System.out.println("Zgloszenie klienta: " + socket);
                //oddajemy klientowi do dyspozycji watek serwera
                WatekGracza watek = new WatekGracza(socket);
                //WatekGracza.watkiGracze.add(watek);
                watek.start();
                //zamkniÄ™ciem socketu zajmie siÄ™ wÄ…tek
                
                //Przebieg gry
                PrzebiegGry.zacznijGre();
            }
        } catch (Exception e) {
            Serwer.logi.append("Blad: " + e + " \n");
            // poniższe by widzieć ostatni wpis do JTextArea
            Serwer.logi.setCaretPosition(Serwer.logi.getText().length() - 1);
            Serwer.logi.repaint();
        }
    }
}
/** Klasa tworzy okno serwera
     */ 
public class Serwer extends JFrame implements ActionListener
{
    static JButton wlaczSerwer;
    static JLabel eStatusSerwera;
    private JLabel ePortSerwera;
    static JTextField portSerwera;
    static JTextArea logi;
    private JPanel panelSrodek, panelLogi, panelUstawien;
    private JMenuBar menuBar;
    private JMenu menuGry;
    private JMenuItem mIHasla, mIKoniec;
    static String filename = "hasla.txt";
    /** Zmienna haslaDoZgadywania typu ArrayList<String>. przechowuje liste hasel
    */ 
    static ArrayList<String> haslaDoZgadywania = new ArrayList<String>();
    /** Zmienna wUruchomienia typu WatekUruchomienia. przechowuje watek odpowiedzialny za przylaczanie nowych graczy
    */ 
    static WatekUruchomienia wUruchomienia;
    
    public static void main(String[] args) 
    {
        Serwer okno = new Serwer("Kalambury - Serwer");
        okno.init();
        okno.setVisible(true);
    }
    Serwer(String tytul) 
    {
        super(tytul);
    }
    void init() 
    {
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);
        
        menuBar = new JMenuBar();
        menuGry = new JMenu("Gra");
        mIHasla = new JMenuItem("Hasła do zgadywania");
        mIHasla.addActionListener(this);
        mIKoniec = new JMenuItem("Zakończ");
        mIKoniec.addActionListener(this);
        menuGry.add(mIHasla);
        menuGry.add(mIKoniec);
        menuBar.add(menuGry);
        setJMenuBar(menuBar);
        
        Component bpion = Box.createHorizontalStrut(10);
        Component bpion2 = Box.createHorizontalStrut(10);
        Component bpoziom = Box.createVerticalStrut(10);
        add(bpion, BorderLayout.EAST);
        add(bpion2, BorderLayout.WEST);
        add(bpoziom, BorderLayout.NORTH);
        
        panelSrodek = new JPanel();
        panelSrodek.setLayout(new BorderLayout());
        add(panelSrodek, BorderLayout.CENTER);
        
        panelUstawien = new JPanel();
        panelUstawien.setLayout(new FlowLayout());
        panelSrodek.add(panelUstawien, BorderLayout.NORTH);
        
        panelLogi = new JPanel();
        panelLogi.setLayout(new BorderLayout());
        panelSrodek.add(panelLogi, BorderLayout.CENTER);

        
        logi = new JTextArea();
        logi.setBorder(new TitledBorder("Logi: "));
        panelLogi.add(logi,BorderLayout.CENTER);
        panelLogi.add(new JScrollPane(logi));
        
        ePortSerwera = new JLabel("Port Serwera:");
        panelUstawien.add(ePortSerwera);
        portSerwera = new JTextField(6);
        panelUstawien.add(portSerwera);
        wlaczSerwer = new JButton("START");
        panelUstawien.add(wlaczSerwer);
        wlaczSerwer.addActionListener(this);
        eStatusSerwera = new JLabel("Serwer wyłączony");
        panelUstawien.add(eStatusSerwera);
        eStatusSerwera.setForeground(Color.red);
     
        wczytajHasla();
    }
    public void actionPerformed(ActionEvent zdarzenie)
    {
        Object zrodlo = zdarzenie.getSource();
        if (zrodlo == wlaczSerwer)
        {
            if(wlaczSerwer.getText().equals("START") && portSerwera.getText().length()!=0)
            {
                wUruchomienia = new WatekUruchomienia();
                wUruchomienia.start();
            }
        }
        if (zrodlo == mIKoniec)//Jesli z menu wybralismy Koniec, zamykamy serwer
        {
            try 
            {
               wUruchomienia.s.close();
            } 
            catch (Exception e) {} 
            this.dispose();
        }
        if (zrodlo == mIHasla)
        {
            HaslaOkno haslaOkno = new HaslaOkno();
            haslaOkno.init();
            haslaOkno.setVisible(true);
        }
    }
    /** Metoda wczytuje hasla
    */ 
    static void wczytajHasla()
    {    
      try 
        {
            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(filename)));
                haslaDoZgadywania.clear();
            
            String linia;
            while(true) 
            {                
                linia = in.readLine();
                if(linia == null) break;
                
                haslaDoZgadywania.add(linia);                
            }
            in.close(); 
            if(haslaDoZgadywania.size()>0)
            {
                logi.append("Wczytano: " + haslaDoZgadywania.size() + " haseł \n");
                // poniższe by widzieć ostatni wpis do JTextArea
                Serwer.logi.setCaretPosition(Serwer.logi.getText().length() - 1);
                Serwer.logi.repaint();
            }
        } 
        catch(Exception e) 
        { 
            System.out.println(e); 
            logi.append("Nie wczytano żadnego hasła :(");
            // poniższe by widzieć ostatni wpis do JTextArea
            Serwer.logi.setCaretPosition(Serwer.logi.getText().length() - 1);
            Serwer.logi.repaint();
        }                
    }
}
/** Klasa odpowiada za poprawne zamkniecie serwera
*/ 
class Zamykacz extends WindowAdapter 
{
 public void windowClosing(WindowEvent e) 
    {
        try 
        {
            Serwer.wUruchomienia.s.close();
        } 
        catch (IOException ex) {}
    }
}

