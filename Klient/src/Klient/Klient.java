package Klient;

/** Kalambury - Klient
* Projekt do kursu <b>Programowanie w Sieci Internet</b> 
* @author Damian Fyda
* @author dfyda@wsb-nlu.edu.pl 
* @version 1.2 lipiec 2014 
*/
import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.logging.*;
import javax.imageio.ImageIO;
import javax.swing.table.*;
import javax.xml.bind.DatatypeConverter;

/** Klasa steruje czasem rysowania
     */ 
class Zegar extends Thread
{
    /** Zmienna czas typu int służąca do przechowywania czasu rysowania
    */ 
    int czas = 180;
    public void run()
    {
        while(true)
        {
            if(Klient.czyRysuje == true && czas > 0)
            {
                KlientOkno.czasRysowania.setText(--czas + " sekund");
                KlientOkno.czasRysowania.repaint();
                
                if(czas == 100)
                {
                   Klient.out.println("<<<<PODPOWIEDZ>>>>" + podpowiedz(1));
                }
                if(czas == 70)
                {
                   Klient.out.println("<<<<PODPOWIEDZ>>>>" + podpowiedz(2));
                }
                if(czas == 30)
                {
                   Klient.out.println("<<<<PODPOWIEDZ>>>>" + podpowiedz(3));
                }
            }
            else if(czas == 0)
            {
                Klient.out.println("<<<<REZYGNUJE>>>>");
                KlientOkno.jPanel1.setVisible(false);//Po rezygnacji ukryj panel rysowania
                KlientOkno.nowyKomponentRysowania();
                restartujZegar();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Zegar.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    /** 
    Metoda przywraca domyslny czas rysowania
    */
    private void restartujZegar()
    {
        czas = 180;
    }
    /** 
    Metoda generuje podpowiedz
    @return zwraca podpowiedz
    */
    private String podpowiedz(int iloscLiter)
    {
        String tmp = KlientOkno.coRysujesz.getText();
        char[] literki = tmp.toCharArray();
        String podpowiedz = "";
        int i = 0;
        while(i<iloscLiter && literki.length > i)
        {
            podpowiedz += literki[i];
            i++;
        }
        podpowiedz +="...";
        return podpowiedz;
    }
}
/** Klasa odpowiada za okno łączenia sie ze serwerem
     */ 
class NowaGra extends JDialog implements ActionListener
{
    private JTextField port,ip;
    private JLabel ePortSerwera,eIPSerwera;
    private JButton start;
    NowaGra()
    {
        super();
    }
    void init() 
    {
        setSize(300, 130);

        setLayout(new BorderLayout());
        setResizable(false);
        
        Component bpion = Box.createHorizontalStrut(10);
        Component bpion2 = Box.createHorizontalStrut(10);
        Component bpoziom = Box.createVerticalStrut(10);
        Component bpoziom2 = Box.createVerticalStrut(10);
        add(bpion, BorderLayout.EAST);
        add(bpion2, BorderLayout.WEST);
        add(bpoziom, BorderLayout.NORTH);
        add(bpoziom, BorderLayout.SOUTH);
        
        JPanel panelSrodek = new JPanel();
        panelSrodek.setLayout(new FlowLayout());
        add(panelSrodek, BorderLayout.CENTER);
        
        ePortSerwera = new JLabel("Port serwera: ");
        panelSrodek.add(ePortSerwera);
        port = new JTextField(10);
        port.setText(String.valueOf(Klient.portSerwera));
        panelSrodek.add(port); 
        eIPSerwera = new JLabel("IP serwera: ");
        panelSrodek.add(eIPSerwera);
        ip = new JTextField(10);
        ip.setText(Klient.adresSerwera);
        panelSrodek.add(ip); 
        start = new JButton("Dołącz do gry");
        start.addActionListener(this);
        panelSrodek.add(start);
    }
    public void actionPerformed(ActionEvent zdarzenie)
    {
        Object zrodlo = zdarzenie.getSource();
        if (zrodlo == start)
        {
            if(!port.getText().isEmpty() && !ip.getText().isEmpty())
            {
                try
                {
                    Klient.out.println("<<<<END>>>>");//rozlaczenie gdy juz bylismy polaczeni
                }
                catch(Exception e){}
                
                Klient.czyscRanking();//Czyszczenie rankingu przed przyjeciem informacji o graczach
                
                Klient.portSerwera=Integer.parseInt(port.getText());
                Klient.adresSerwera=ip.getText();
                try {
                    Klient.iAdres = InetAddress.getByName(Klient.adresSerwera);
                    Klient.okno.chat.append("\nŁączę sie z adresem = " + Klient.iAdres + "\n");
                } catch (Exception e) {
                    System.exit(0);
                }
                try {
                    Klient.socket = new Socket(Klient.iAdres, Klient.portSerwera);
                } catch (IOException e) {
                    Klient.okno.chat.append("Nie udało się uzyskać połączenia\n");
                    return;
                }

                //startuję wątek nasłuchujący 
                Klient.watekKlienta = new WatekKlienta();
                Klient.watekKlienta.start();
                Klient.zegar = new Zegar();
                Klient.zegar.start();
                KlientOkno.menuGry.setEnabled(false);
            }
            this.dispose();
        }
    }
}
/** Klasa odpowiada za watek klienta
*/ 
class WatekKlienta extends Thread 
{   
    public WatekKlienta() 
    {
        // kojarzę strumień z gniazdem 
        try {
            Klient.in = new BufferedReader(
                    new InputStreamReader(
                            Klient.socket.getInputStream()));
            Klient.out = new PrintWriter(
                            new BufferedWriter(
                                    new OutputStreamWriter(
                                            Klient.socket.getOutputStream())), true);
            
        } catch (IOException e) {
        }
    }
    public void run() 
    {
        String str = null;
        
        try {
            while ((str = Klient.in.readLine()) != null) 
            {
                if(str.contains("<<PIERWSZYGRACZ>>"))//Jesli pierwszy gracz to czyszcze ranking
                    Klient.czyscRanking();
                if(str.contains("<<GRACZ>>"))//Jesli gracz to przekazuje informacje do tabeli
                {
                    String[] tmp = str.split("<<GRACZ>>");
                    tmp = tmp[1].split(" ");
                    Klient.okno.tabela.addRow(tmp);
                }
                if(str.contains("<<WIADOMOSC>>"))//Jesli wiadomosc, przekazuje do Czatu
                {
                    if(str.contains("ODGADŁ HASŁO"))
                        KlientOkno.nowyKomponentRysowania();//Resetuje okno rysowania
                    if(str.contains("REZYGNUJE"))
                        KlientOkno.nowyKomponentRysowania();//Resetuje okno rysowania
                    String[] tmp = str.split("<<WIADOMOSC>>");//wyczyszczenie z tagu
                    Klient.okno.chat.append("\n" + tmp[1]);
                    // poniższe by widzieć ostatni wpis do JTextArea
                    Klient.okno.chat.setCaretPosition(Klient.okno.chat.getText().length() - 1);
                    Klient.okno.chat.repaint();
                }
                if(str.contains("<<GRA>>"))//jesli gra, ukrywam komponenty rysowania
                {
                    Klient.czyRysuje = false;
                    Klient.okno.jPanel1.setVisible(false);
                    Klient.okno.wiadomosc.setVisible(true);
                    Klient.okno.jScrollPane1.repaint();
                }
                if(str.contains("<<HASLO>>"))//jesli haslo, przygotowuje komponent rysowania i pozwalam rysować
                {
                    Klient.czyRysuje = true;
                    KlientOkno.nowyKomponentRysowania();
                    String[] tmp = str.split("<<HASLO>>");//wyczyszczenie z tagu
                    Klient.okno.coRysujesz.setText(tmp[1]);
                    Klient.okno.jPanel1.setVisible(true);
                    Klient.okno.wiadomosc.setVisible(false);
                    Klient.okno.jScrollPane1.repaint();
                }
                if(str.contains("<<OBRAZ>>"))//jesli obraz przekazuje go to komponentu rysowania
                {
                    if(Klient.czyRysuje == false)
                    {
                        String[] tmp = str.split("<<OBRAZ>>");//wyczyszczenie z tagu
                        String tmp2 = tmp[1];
                        Image img = Klient.bityNaObraz(tmp2);//konwertuje otrzymane dane na obraz
                        KlientOkno.jPanel2.rysowanyObrazek=img;
                        KlientOkno.jPanel2.repaint();
                    }
                }         
            }
            Klient.in.close(); //Zamykam strumień wejściowy od serwera 
        } catch (IOException e) {
            String wyjatek = e.toString();
            if(wyjatek.contains("java.net.SocketException: Connection reset"))
                Klient.okno.chat.append("\nBrak połączenia z serwerem");
            else
                Klient.okno.chat.append("\nBłąd: " + e);
        }
    }
}
/** Klasa tworzy klienta
*/ 
public class Klient
{
    static PrintWriter out;
    static BufferedReader in; 
    /** Zmienna portSerwera typu int przechowuje port serwera, domyslny port to 12345
    */ 
    static int portSerwera = 12345;
    /** Zmienna adresSerwera typu String przechowuje adres IP serwera, domyslne IP to 127.0.0.1
    */ 
    static String adresSerwera = "127.0.0.1";;
    static InetAddress iAdres = null;
    static Socket socket = null;
    /** Zmienna watekKlienta typu WatekKlienta. przechowuje watek odpowiedzialny za komunikacje z serwerem
    */ 
    static WatekKlienta watekKlienta;
    static KlientOkno okno;
    static boolean czyRysuje = false;
    /** Zmienna zegar typu Zegar. przechowuje watek odpowiedzialny za czas rysowania
    */
    static Zegar zegar;

    public static void main(String args[]) 
    {
       Klient klient = new Klient();
       klient.okno = new KlientOkno();
       klient.okno.setVisible(true); 
    }
    /** Metoda czysci tabele zawierającą ranking
    */ 
    static public void czyscRanking()
    {
        while (okno.tabela.getRowCount() > 0) 
            okno.tabela.removeRow(okno.tabela.getRowCount() - 1);
        //Czyszczenie tabeli przed ponownym logowaniem 
    }
    /** Metoda odpowiedzialna za wyslanie obrazu do serwera
    */ 
    static public void wyslijObraz(Image image) throws IOException
    {
        if(Klient.czyRysuje == true)
        {
            String obrazek = "<<OBRAZ>>";
            obrazek += DatatypeConverter.printBase64Binary(obrazNaBity(image));//Konwersja obrazka do Base64Binary 
            out.println(obrazek);
        }
    }
    /** Metoda konwertuje obraz na bity
    @return zwraca obrazek w postaci bitów 
    */ 
    static byte[] obrazNaBity(Image image) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedImage originalImage = doBuforowanegoObrazu(image);
	ImageIO.write( originalImage, "jpg", baos );
	baos.flush();
	byte[] imageInByte = baos.toByteArray();
	baos.close();
        return imageInByte;
    }
    /** Metoda konwertuje bity na obraz
    @return zwraca obrazek
    */
    static Image bityNaObraz(String image) throws IOException
    {
        byte[] rysunek = DatatypeConverter.parseBase64Binary(image); 
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(rysunek));
        return img;
    }
    /** Metoda konwertuje obraz na obraz buforowany, potrzebny do zamiany obrazu na bity
    @return zwraca buforowany obrazek 
    */
    static BufferedImage doBuforowanegoObrazu(Image img)//Konwersja image do BufferedImage
    {
       //Twożenie bitmapy 
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Rysowanie na bitmapie
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        return bimage;
    }
}
/** Klasa odpowiada za poprawne zamknięcie klienta
*/ 
class Zamykacz extends WindowAdapter 
{
    private KlientOkno okno;

    Zamykacz(KlientOkno okno) 
    {
        this.okno = okno;
    }
    public void windowClosing(WindowEvent e) 
    {
        if(Klient.out !=null)
            Klient.out.println("<<<<END>>>>");
        System.exit(0);
    }
}
/** Klasa tworzy okno klienta
     */ 
class KlientOkno extends JFrame implements ActionListener, KeyListener
{
    static JTextArea chat;
    static JLabel coRysujesz;
    static JLabel czasRysowania;
    static JPanel jPanel1;
    static KomponentRysowania jPanel2;
    private JPanel jPanel3;
    static JScrollPane jScrollPane1;
    private JScrollPane jScrollPane2;
    private JScrollPane jScrollPane3;
    private JSeparator jSeparator1;
    private JSeparator jSeparator2;
    static JMenuBar menu;
    private JLabel naglowekRankingu;
    private JLabel naglowekRozmowy;
    private JLabel nazwStolu;
    static JMenuItem nowaGra;
    static JMenu menuGry;
    static JTable ranking;
    private JButton rezygnuje;
    static JTextField wiadomosc;
    static DefaultTableModel tabela;
    static JButton pisak;
    static JButton gumka;
    
   public KlientOkno() 
    {
        super("Kalambury - Klient");
        this.init();
    }                     
    private void init() 
    {
        jPanel1 = new JPanel();
        coRysujesz = new JLabel();
        czasRysowania = new JLabel();
        rezygnuje = new JButton();
        jPanel2 = new KomponentRysowania();
        jPanel3 = new JPanel();
        nazwStolu = new JLabel();
        jSeparator1 = new JSeparator();
        naglowekRozmowy = new JLabel();
        jScrollPane1 = new JScrollPane();
        chat = new JTextArea();
        jScrollPane2 = new JScrollPane();
        wiadomosc = new JTextField();
        jSeparator2 = new JSeparator();
        naglowekRankingu = new JLabel();
        jScrollPane3 = new JScrollPane();
        ranking = new JTable();
        menu = new JMenuBar();
        nowaGra = new JMenuItem();
        pisak = new JButton("Pisak");
        gumka = new JButton("Gumka");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jPanel1.setBackground(new Color(255, 204, 153));

        coRysujesz.setFont(new Font("Tahoma", 1, 14)); // NOI18N
        coRysujesz.setText("Rysujesz: slowo");

        czasRysowania.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        czasRysowania.setText("??");

        rezygnuje.setText("Rezygnuje");
        rezygnuje.addActionListener(this);
        
        pisak.addActionListener(this);
        gumka.addActionListener(this);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(coRysujesz)
                .addGap(48, 48, 48)
                .addComponent(czasRysowania)
                .addGap(10, 10, 10)
                .addComponent(pisak)
                .addGap(5, 5, 5)
                .addComponent(gumka)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                .addComponent(rezygnuje)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(coRysujesz)
                    .addComponent(czasRysowania)
                    .addComponent(pisak)
                    .addComponent(gumka)
                    .addComponent(rezygnuje))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new Color(255, 255, 255));
        
        pisak.addActionListener(this);
        gumka.addActionListener(this);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel3.setBackground(new Color(0, 153, 204));

        nazwStolu.setFont(new Font("Tahoma", 1, 14)); // NOI18N
        nazwStolu.setText("Stół #1");

        naglowekRozmowy.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        naglowekRozmowy.setForeground(new Color(255, 255, 255));
        naglowekRozmowy.setText("Rozmowa");

        chat.setEditable(false);
        jScrollPane1.setViewportView(chat);

        wiadomosc.setAutoscrolls(false);
        jScrollPane2.setViewportView(wiadomosc);
        wiadomosc.getAccessibleContext().setAccessibleParent(null);
        wiadomosc.addKeyListener(this);

        naglowekRankingu.setFont(new Font("Tahoma", 1, 11)); // NOI18N
        naglowekRankingu.setForeground(new Color(255, 255, 255));
        naglowekRankingu.setText("Ranking");

        tabela=new DefaultTableModel(); 
        tabela.addColumn("Nazwa");
        tabela.addColumn("Punkty");
        ranking = new JTable(tabela);
        ranking.setEnabled(false);
        RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tabela);
        sorter.toggleSortOrder(1);
        ranking.setRowSorter(sorter);
       
        jScrollPane3.setViewportView(ranking);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2)
                    .addComponent(jSeparator1)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nazwStolu)
                            .addComponent(naglowekRozmowy)
                            .addComponent(naglowekRankingu))
                        .addGap(0, 207, Short.MAX_VALUE))
                    .addComponent(jScrollPane3)
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(nazwStolu)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(naglowekRozmowy)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(naglowekRankingu)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        menu = new JMenuBar();
        menuGry = new JMenu("Gra");
        nowaGra = new JMenuItem("Nowa Gra");
        nowaGra.addActionListener(this);
        menuGry.add(nowaGra);
        menu.add(menuGry);
        setJMenuBar(menu);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();//Układa wygląd
        jPanel1.setVisible(false);
        Zamykacz stroz = new Zamykacz(this);
        addWindowListener(stroz);
    }

    public void actionPerformed(ActionEvent zdarzenie) 
    {
        Object zrodlo = zdarzenie.getSource();
        if (zrodlo == nowaGra)
        {
            NowaGra nowaGra = new NowaGra();
            nowaGra.init();
            nowaGra.setVisible(true);
        }
        if (zrodlo == rezygnuje)
        {
            Klient.out.println("<<<<REZYGNUJE>>>>");
            jPanel1.setVisible(false);//Po rezygnacji ukryj panel rysowania
            nowyKomponentRysowania();
        }
        if (zrodlo == pisak)
        {
            jPanel2.olowek();
        }
        if (zrodlo == gumka)
        {
            jPanel2.gumka();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) 
    {
    }

    @Override
    public void keyPressed(KeyEvent e) 
    {
        if(e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            String odpowiedz = "<<ODPOWIEDZ>>" + wiadomosc.getText();
            Klient.out.println(odpowiedz);
            wiadomosc.setText("");
        }
    }
    @Override
    public void keyReleased(KeyEvent e) 
    {
    } 
    /** Metoda resetuje komponent rysowania
     */ 
    static void nowyKomponentRysowania()
    {
        jPanel2.rysowanyObrazek = null;
        jPanel2.repaint();
    }
}

/** Klasa tworzy i zarządza komponentem rysowania
*/ 
class KomponentRysowania extends JComponent
{
    /** Rysowany obrazek
    */ 
    public Image rysowanyObrazek;
    public Graphics2D graphics2D;
    /** Wspolrzedne rysowania linii
    */ 
    private int obecnyX, obecnyY, staryX, staryY;
    /** Rodzaj kursora, 7 - gumka, 1 - ołówek
    */ 
    private int kursor;
    /** Wspolrzedne rysowania prostokąta(białej gumki)
    */ 
    private int e1, f1, e2, f2;//Wspolrzedne prostokata(gumki)
    KomponentRysowania()
    {
        super();
    }
    /** Metoda pozwala na rysowanie ołówkiem
    */ 
    public void olowek()
        {
            if(Klient.czyRysuje == true)
            {
                czarny();//ustawianie czarnego koloru pisaka
                grubosc();//ustawianie grubosci pisaka
                kursor = 1;
                setDoubleBuffered(false);
                zmienKursor();//Przywracanie kursora ołówka

                addMouseListener(new MouseAdapter()//Jesli myszka jest kliknieta, ustaw stare X i Y
                {
                    public void mousePressed(MouseEvent e) 
                    {
                        staryX = e.getX();
                        staryY = e.getY();
                    }
                });
                addMouseMotionListener(new MouseMotionAdapter()//Rysowanie obrazka 
                {
                    public void mouseDragged(MouseEvent e)//Jesli ciągnie myszke
                    {
                        obecnyX = e.getX();
                        obecnyY = e.getY();
                        if (graphics2D != null && kursor == 1)
                        {
                            graphics2D.drawLine(staryX, staryY, obecnyX, obecnyY);//Rysuj kreske od starych do nowych wspolrzednych
                        }
                        repaint();
                        staryX = obecnyX;
                        staryY = obecnyY;
                        repaint();
                        try {
                            Klient.wyslijObraz(rysowanyObrazek);//wysylamy obraz do serwera
                        } catch (IOException ex) {
                            Logger.getLogger(KomponentRysowania.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            }
    }
    public void paintComponent(Graphics g)
    {
        if(rysowanyObrazek == null)
        {
            rysowanyObrazek = createImage(getSize().width, getSize().height);
            graphics2D = (Graphics2D)rysowanyObrazek.getGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            czysc();//Czyszczenie obrazu(Caly przemalowany na bialo)
        }
        g.drawImage(rysowanyObrazek, 0, 0, null);//Rysowanie obrazka
    }
    /** Metoda pozwala na czyszczenie rysunku gumką
    */ 
    public void gumka()
    {
        if(Klient.czyRysuje == true)
        {
           kursor = 7;//Kursor gumki

           zmienKursor();//zmiana kursora
           graphics2D.setPaint(Color.white);//Ustawianie bialego koloru rysowania
           addMouseListener(new MouseAdapter() 
           {
               public void mousePressed(MouseEvent e)//Jesli myszka jest kliknieta, ustaw stare X i Y
               {
                   e1 = e.getX();
                   f1 = e.getY();
               }
           });
           addMouseMotionListener(new MouseMotionAdapter() 
           {
               public void mouseDragged(MouseEvent e) 
               {
                   e2 = e.getX();
                   f2 = e.getY();
                   if (graphics2D != null && kursor == 7) 
                   {
                       graphics2D.fillRect(e1, f1, 27, 27);//Rysuj wypelniony prostokat
                   }
                   repaint();
                   e1 = e2;
                   f1 = f2;
                   try {
                           Klient.wyslijObraz(rysowanyObrazek);
                       } catch (IOException ex) {
                           Logger.getLogger(KomponentRysowania.class.getName()).log(Level.SEVERE, null, ex);
                       }
               }
           });
        }
    }
    /** Metoda czysci obrazek(maluje go na biało)
    */ 
    public void czysc()
    {
        zmienKursor();//zmiana kursora na ołówek
        graphics2D.setPaint(Color.white);
        graphics2D.fillRect(0, 0, getSize().width, getSize().height);
    }
    /** Metoda zmienia kursor myszki
    */ 
    public void zmienKursor()
    {
        if(kursor==7)//Ustaw kursor gumki
        {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Image image = toolkit.getImage("gumka.png");
            Point hotSpot = new Point(0, 0);
            Cursor cursor = toolkit.createCustomCursor(image, hotSpot, "Gumka");
            setCursor(cursor); 
        }
        else 
            setCursor (Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));//ustawienie kursora myszki
    }
    /** Metoda ustawia grubosc ołówka
    */ 
    public void grubosc()
    {
        graphics2D.setStroke(new BasicStroke (1));
    }
    /** Metoda ustawia czarny kolor ołówka
    */ 
    public void czarny()
    {
        graphics2D.setPaint(Color.black);
        repaint();
    }
}
