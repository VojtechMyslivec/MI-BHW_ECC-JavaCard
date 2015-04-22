/** 
 * Implementace ECC v projektivnich souradnicich nad GF(p^n) 
 * pro JavaCard
 *
 * @
 *    Kostra programu dle [1]
 *
 * Autori
 *    Vojtech Myslivec <vojtech.myslivec@fit.cvut.cz>
 *    Zdenek Novy      <zdenek.novy@fit.cvut.cz>
 *  FIT CVUT v Praze, LS 2015
 *
 *
 *  Reference:
 *     [1]     Bucek, Jiri. JavaCard v Bezpecnost a technicke prostredky. 
 *             FIT CVUT v Praze, 2015 
 *             <https://edux.fit.cvut.cz/courses/MI-BHW/lectures/07/start>
 *     [2]     Novotny, Martin. ECC, Arithmetics over GF(p) and GF(2^m) 
 *             v Bezpecnost a technicke prostredky. FIT CVUT v Praze, 2015
 *             <https://edux.fit.cvut.cz/courses/MI-BHW/_media/lectures/elliptic/mi-bhw-6-gfarithmetic.pdf>
 *     [3]     Merchan J. G., Kumar S., Paar C., Pelzl J.
 *             Efficient Software Implementation of Finite Fields with 
 *             Applications to Cryptography v Acta Applicandae Mathematicae: 
 *             An International Survey Journal on Applying Mathematics and
 *             Mathematical Applications, Volume 93, Numbers 1-3, pp. 3-32,
 *             September 2006. Ruhr-Universitat Bochum, 2006.
 *             <http://www.emsec.rub.de/research/publications/efficient-software-implementation-finite-fields-ap/>
 *     [4]     IEEE Standard Specifications for Public-Key Cryptography,
 *             IEEE Std 1363-2000. IEEE Computer Society, 2000.
 *             <http://ieeexplore.ieee.org/xpl/mostRecentIssue.jsp?punumber=7168>
 *
 */

package pack1;

import javacard.framework.*;

/**
 *
 * @author novyzde3, myslivo1, bucekj
 */
public class Applet1 extends Applet {
    // TODO
    // jePoleNula ( (XYZ).hodnoty ) nahradit za \1.jePolynomNula( )
    
    /************************* cPolynom ****************************/
    private class cPolynom {
        // TODO private
        public byte[] hodnoty;
        
        public cPolynom() {
            hodnoty = vytvorPrazdnePole( DELKAPOLE );
        }
        
        /*public ~cPolynom() {
            hodnoty = null;
            JCSystem.requestObjectDeletion();
        }*/

//        // kopie A
//        public static byte[] vytvorKopiiPolynomu( final byte[] A ) {
//            byte[] B = vytvorPrazdnePole();
//            priradPolynom( B, A );
//            return B;
//        }

        public boolean jePolynomNula() {
            return jePoleNula( hodnoty );
        }
        
        // A <- B
        public boolean priradPolynom( final cPolynom B ) {
            zkopirujPole( hodnoty, B.hodnoty );
            return true;
        }

        // A <- b
        public boolean priradPolynom( final byte[] b ) {
            zkopirujPole( hodnoty, b );
            return true;
        }
        
        // A <- A xor B
        public boolean prictiPolynom( final cPolynom B ) {
            prictiPole( hodnoty, B.hodnoty );
            return true;
        }

        // A <- A * B mod P
        // Comb metoda, dle [3], alg. 2 
        public boolean prinasobPolynom( final cPolynom B ) {
            // aktualni hodnoty pole, v prubehu vypoctu se posouvaji max o 1 B
            //a = vytvorPrazdnePole( (byte)( DELKAPOLE + 1 ) );
            vynulujPole( a );
            zkopirujPole( a, hodnoty );
            // jen ukazatel
            byte[] b = B.hodnoty;
            // v c postupne vznika vysledek
            //byte[] c = vytvorPrazdnePole( DELKAPOLEkrat2 );
            vynulujPole( c );

            // nasobeni ----------------------------------------
            // cyklus pres bity
            for ( byte j = 0 ; j < 8 ; j++ ) {
                // cyklus pres bajty
                for ( byte i = 0 ; i < DELKAPOLE ; i++ ) {
                    if ( bitZPole( b, (byte) ( i*8 + j ) ))
                        prictiPole( c, a, i );
                }
                if ( ! posunPoleDoleva( a ) ) {
                    // nesmi nasat, ze by nejaky bit vypadl. Znamenalo by to,
                    // ze se nasobilo necim vice nez x^79
                    return false;
                }
            }
            // redukce -----------------------------------------
            //if ( ! redukujDvojnasobnyPolynom( hodnoty, c ) ) {
            if ( ! redukujDvojnasobnyPolynom( c ) ) {
                // Nemelo by nastat. Znamenalo by to, ze byl 
                // nejvyssi bit nastaven na 1
                return false;
            }
            
            return true;
        }
        
        // A <- A^2 mod P
        // expanze -- prolozeni nulami -- a nasledna redukce, dle [2], 
        // Squaring over GF(2^m) with polynomial basis
        public boolean umocniPolynom( ) {
            //byte[] C = vytvorPrazdnePole( DELKAPOLEkrat2 );
            byte   nibble;
            // expanze -----------------------------------------
            for ( byte i = DELKAPOLE-1 ; i >= 0 ; i-- ) {
                // spodni nibble
                nibble   = (byte) (( hodnoty[i]      ) & 0x0F);
                c[2*i]   = expanze[nibble];
                // horni nibble
                nibble   = (byte) (( hodnoty[i] >> 4 ) & 0x0F);
                c[(byte)(2*i+1)] = expanze[nibble];
            }

            return redukujDvojnasobnyPolynom( c );
        }
        
         // A <- c mod P; inspirace z [3], alg. 4
        // Redukuje pole B max do delky 2*DELKAPOLE, ale nejvyssi bit 
        // dvojnasobneho pole musi byt 0, jinak neredukuje vse.
        // To by nemelo nastat ani po nasobeni ani po mocneni.
        // Vraci false v pripade, ze je tento bit 1 -- tedy v pripade spatneho
        // vysledku.
        // Jinak true.
        public boolean redukujDvojnasobnyPolynom( final byte[] c ) {
            byte horniCast, spodniCast, bajt, index;
//            byte[] C = vytvorPrazdnePole( DELKAPOLEkrat2 );
//            zkopirujPole( C, B );

            for ( index = DELKAPOLEkrat2 - 1 ; index >= DELKAPOLE; index-- ) {
                horniCast  = (byte) (( c[index]   <<      B1 ) & (byte) 0xFE);
                spodniCast = (byte) (( c[index-1] >> (byte)7 ) & (byte) 0x01);
                bajt       = (byte) (horniCast ^ spodniCast);
                // pro x^9 ..........................
                // horni bit
                horniCast  = (byte) ( (bajt >> 7) & 0x01 );
                // sedm spodnich bitu
                spodniCast = (byte) ( (bajt << 1) & 0xFE );
                // horni  cast -- xor k hodnotam o 8 bytu nize,
                c[index-8] ^= horniCast;
                // spodni cast -- xor k hodnotam o 9 bytu nize;
                c[index-9] ^= spodniCast;

                // pro x^0 ..........................
                // zde je to jednoduche
                c[index-10] ^= bajt;
            }
            // Vynulovani bitu x^79 po redukci (v cyklu)
            c[DELKAPOLE-1] &= 0x7F;

            index      = DELKAPOLEkrat2;
            spodniCast = (byte) (( c[index-1] >> 7 ) & 0x01);
            // vysledek nakopiruje do hodnot
            zkopirujPole( hodnoty, c, DELKAPOLE );
            
            return ( spodniCast == 0x00 );
        }
        /******************************************************************/
    }
    
    /***************************************************************/
    
    /*************************** cBod ******************************/
    private class cBod {
        private cPolynom X;
        private cPolynom Y;
        private cPolynom Z;
        
        public cBod( ) {
            X = new cPolynom();
            Y = new cPolynom();
            Z = new cPolynom();
        }
        
        public void priradBod( final cBod Q ) {
            X.priradPolynom(Q.X);
            Y.priradPolynom(Q.Y);
            Z.priradPolynom(Q.Z);
        }
        
        public void priradNekonecno( ) {
            X.priradPolynom( polynomJedna );
            Y.priradPolynom( polynomJedna );
            Z.priradPolynom( polynomNula );
        }
        
        // P <- 2*P; algoritmus dle [4], A.10.6
        // ( v Q je totozny bod s P )
        public void zdvojBod( ) {
            //if ( jePoleNula( X.hodnoty ) || jePoleNula( Z.hodnoty ) ) { // 5.
            if ( X.jePolynomNula() || Z.jePolynomNula() ) { // 5.
                // Vysledek bude bod v nekonecnu
                priradNekonecno();
                return;
            }

            T4.priradPolynom( X );      // 1.
            X.priradPolynom( ECc );     // 4.

            Y.prinasobPolynom( Z );     // 6.
            Z.umocniPolynom( );         // 7.
            
            X.prinasobPolynom( Z );     // 8.
            Z.prinasobPolynom( T4 );    // 9.    // vypocte Z2

            Y.prictiPolynom( Z );       // 10.
            X.prictiPolynom( T4 );      // 11.
            X.umocniPolynom( );         // 12.
            X.umocniPolynom( );         // 13.   // vypocte X2

            T4.umocniPolynom( );        // 14.
            Y.prictiPolynom( T4 );      // 15.   // vypocte U
            Y.prinasobPolynom( X );     // 16.
            T4.umocniPolynom( );        // 17.
            T4.prinasobPolynom( Z );    // 18.
            Y.prictiPolynom( T4 );      // 19.   // vypocte Y2
        }

        // P <- P + Q; algoritmus dle [4], A.10.7, A.10.8
        public void prictiBod( final cBod Q ) {
            // algoritmus A.10.8 ========================================
            // osetreni podminek, kdy se scita s nulou
            // P <- P + 0 
            if ( jePoleNula( Q.Z.hodnoty ) ) { 
                return;
            }
            // P <- 0 + Q
            //if ( jePoleNula( Z.hodnoty ) ) {
            if ( Z.jePolynomNula() ) {
                priradBod( Q );
                return;
            }

            // dale jiz algoritmus A.10.7 ===============================
            //      X = xP = x0 = T1        Q.X = xQ = x1 = T4
            //      Y = yP = y0 = T2        Q.Y = yQ = y1 = T5
            //      Z = zP = z0 = T3        Q.Z = zQ = z1 = T6
    //        byte[] T7 = vytvorKopiiPolynomu( zQ );
            T7.priradPolynom( Q.Z );

            // IMHO neni potreba podminka, protoze se jinak nasobi polynomem 1... mozna se to jen zrychli
            if ( ! jsouPoleStejna( Q.Z.hodnoty, polynomJedna ) ) { // 7.
                T7.umocniPolynom( );
                X.prinasobPolynom( T7 );  // vypocte U0
                T7.prinasobPolynom( Q.Z );
                Y.prinasobPolynom( T7 );  // vypocte S0            
            }
            T7.priradPolynom( Z );
            T7.umocniPolynom( );        // 8.

            T8.priradPolynom( T7 );     // 9a.
            T8.prinasobPolynom( Q.X );  // 9.   // vypocte U1
            X.prictiPolynom( T8 );    // 10.  // vypocte W

            T7.prinasobPolynom( Z );  // 11.

            T8.priradPolynom( T7 );    // 12a.
            T8.prinasobPolynom( Q.Y );  // 12.  // vypocte S1
            Y.prictiPolynom( T8 );    // 13.  // vypocte R

            // pokud vyjde xP nula, jedna se o P +- P
            //if ( jePoleNula( X.hodnoty ) ) {   // 14.
            if ( X.jePolynomNula() ) {   // 14.
                // pokud je i yP nula, jedna se o zdvojovani bodu
                //if ( jePoleNula( Y.hodnoty ) ) {
                if ( Y.jePolynomNula() ) {
                    // priradi puvodni souradnice
                    // TODO stoji za zvazeni, jestli se nepokazi normalizace bodu?!
                    priradBod( Q );
                    // zdvojeni bodu algoritmem A.10.6 ==================
                    zdvojBod( );
                }
                // jinak se jedna o bod O, znormalizuje ho
                else {
                    // TODO Slo by resit tak, ze else lze vynechat a vzdy zavolat 
                    // zdvojBod. Podminka ve zdvojBod dela to same (vysledek 
                    // v nekonecnu).
                    priradNekonecno();
                }
                
                return;
            }

            Z.prinasobPolynom( X );  // 16.  // vypocte L a Z2 pokud Z1==1
            
            T4.priradPolynom( Q.X );    // 15a.
            T5.priradPolynom( Q.Y );    // 17a.
            
            T4.prinasobPolynom( Y );    // 15.
            T5.prinasobPolynom( Z );    // 17.
            T4.prictiPolynom( T5 );     // 18.  // vypocte V

            T5.priradPolynom( Z );      // 19a.
            T5.umocniPolynom( );        // 19. // TODO pozor! v norme je L_2 ale vypocitava se L^2 ... pravd. chyba v indexu 

            T7.priradPolynom( T5 );    // 20a.
            T7.prinasobPolynom( T4 );  // 20.

            // IMHO if neni potreba
            if ( ! jsouPoleStejna( Q.Z.hodnoty, polynomJedna ) ) {   // 21.
                Z.prinasobPolynom( Q.Z );  // vypocte Z2 pokud z1!=1
            }

            T4.priradPolynom( Z );    // 22a.
            T4.prictiPolynom( Y );    // 22.  // vypocte T

            Y.prinasobPolynom( T4 );  // 23.

            T5.priradPolynom( X );    // 24a.
            T5.umocniPolynom( );        // 24
            X.prinasobPolynom( T5 );  // 25

            // toto nastane vzdy, ECa neni nula!
            // imho if neni potreba, jen usetri cas
            if ( ! jePoleNula( ECa ) ) {    // 6. a 26.
                T9.priradPolynom( ECa );    // 6.
                T8.priradPolynom( Z );    // 26.
                T8.umocniPolynom( );
                T9.prinasobPolynom( T8 );
                X.prictiPolynom( T9 );
            }

            X.prictiPolynom( Y );    // 27.  // vypocte X2

            T4.prinasobPolynom( X );  // 28.
            Y.priradPolynom( T4 );    // 29a.
            Y.prictiPolynom( T7 );    // 29.  // vypocte Y2

            return;
        }


        // P <- (+|-)skalar * P; algoritmus double and add (vlastni)
        // pouziva Q (znici ho)
        // kladne
        //     true:  +
        //     false: -
        public void nasobBod( byte[] skalar, boolean kladne ) {
            // Do bodu R ulozime bod this
            // v this bude postupne vznikat vysledek Double & Add
            R.priradBod( this );

            priradNekonecno();

            // vysledek je bod v nekonecnu (1,1,0)
            //if ( jePoleNula( skalar ) || jePoleNula( R.Z.hodnoty ) ) {
            if ( jePoleNula( skalar ) || R.Z.jePolynomNula() ) {
                return;
            }

            // v pripade nasobeni zapornym cislem
            if ( ! kladne ) {
                T7.priradPolynom( R.X );
                T7.prinasobPolynom( R.Z );
                R.Y.prictiPolynom( T7 );
            }

            // index zacina od x^79
            for (byte index = DELKAPOLE * 8 - 1; index >= 0; index--) {
                zdvojBod( );
                if ( bitZPole( skalar, index ) ) {
                    prictiBod( R );
                }
            }
            return;
        }
    
        
        // P <- skalar * P
        public void nasobBod( byte[] skalar ) {
            nasobBod( skalar, true );
        }
    }
    /***************************************************************/
    
    /************************* Konstanty ***************************/
    private static final byte   DELKAPOLE      = (byte)( 10 );
    private static final byte   DELKAPOLEkrat2 = (byte)( 2 * DELKAPOLE );
    private static final byte   DELKAPOLEplus1 = (byte)( 1 + DELKAPOLE );
    // private static final byte   SOURADNIC = (byte)( 3 );
    // private static final byte   X         = (byte)( 0 );
    // private static final byte   Y         = (byte)( 1 );
    // private static final byte   Z         = (byte)( 2 );
    private static final byte   B0 = 0;
    private static final byte   B1 = 1;
    private static final byte   B2 = 2;
    private static final byte   B4 = 4;
    private static final short  S0 = 0;
    private static final short  DOLNICH8BITU = 0x00FF; // smazat?
    private static final byte[] polynomNula  = { B0, B0, B0, B0, B0, B0, B0, B0, B0, B0 };
    private static final byte[] polynomJedna = { B0, B0, B0, B0, B0, B0, B0, B0, B0, B1 };
    private static final byte[] ECa = {
       ( byte ) 0x4A, ( byte ) 0x2E, ( byte ) 0x38, ( byte ) 0xA8, ( byte ) 0xF6,
       ( byte ) 0x6D, ( byte ) 0x7F, ( byte ) 0x4C, ( byte ) 0x38, ( byte ) 0x5F 
    };
    private static final byte[] ECb = { 
       ( byte ) 0x2C, ( byte ) 0x0B, ( byte ) 0xB3, ( byte ) 0x1C, ( byte ) 0x6B,
       ( byte ) 0xEC, ( byte ) 0xC0, ( byte ) 0x3D, ( byte ) 0x68, ( byte ) 0xA7 
    };
    private static final byte[] ECc = {  // c je b^(-1)
       ( byte ) 0x59, ( byte ) 0x79, ( byte ) 0x43, ( byte ) 0x23, ( byte ) 0x3E,
       ( byte ) 0xB3, ( byte ) 0xCA, ( byte ) 0xF9, ( byte ) 0x3E, ( byte ) 0x21 
    };
    
    
    
    // Iniciacni bod D (P)
    /*private static final byte[] xD = {
       ( byte ) 0x30, ( byte ) 0xCB, ( byte ) 0x12, ( byte ) 0x7B, ( byte ) 0x63,
       ( byte ) 0xE4, ( byte ) 0x27, ( byte ) 0x92, ( byte ) 0xF1, ( byte ) 0x07 
    };
    private static final byte[] yD = {
       ( byte ) 0x54, ( byte ) 0x7B, ( byte ) 0x2C, ( byte ) 0x88, ( byte ) 0x26,
       ( byte ) 0x6B, ( byte ) 0xB0, ( byte ) 0x4F, ( byte ) 0x71, ( byte ) 0x3B 
    };
    private static final byte[] zD = { B0, B0, B0, B0, B0, B0, B0, B0, B0, B1 };
    */
    
    private static final byte[] expanze = {
        (byte) 0x00, // '0000' -> '00000000'
        (byte) 0x01, // '0001' -> '00000001'
        (byte) 0x04, // '0010' -> '00000100'
        (byte) 0x05, // '0011' -> '00000101'
        (byte) 0x10, // '0100' -> '00010000'
        (byte) 0x11, // '0101' -> '00010001'
        (byte) 0x14, // '0110' -> '00010100'
        (byte) 0x15, // '0111' -> '00010101'
        (byte) 0x40, // '1000' -> '01000000'
        (byte) 0x41, // '1001' -> '01000001'
        (byte) 0x44, // '1010' -> '01000100'
        (byte) 0x45, // '1011' -> '01000101'
        (byte) 0x50, // '1100' -> '01010000'
        (byte) 0x51, // '1101' -> '01010001'
        (byte) 0x54, // '1110' -> '01010100'
        (byte) 0x55  // '1111' -> '01010101'
    };
    
    /*private byte[] xP; /*= {
       ( byte ) 0x30, ( byte ) 0xCB, ( byte ) 0x12, ( byte ) 0x7B, ( byte ) 0x63,
       ( byte ) 0xE4, ( byte ) 0x27, ( byte ) 0x92, ( byte ) 0xF1, ( byte ) 0x0F 
    };*/
    /*private byte[] yP; /*= {
       ( byte ) 0x54, ( byte ) 0x7B, ( byte ) 0x2C, ( byte ) 0x88, ( byte ) 0x26,
       ( byte ) 0x6B, ( byte ) 0xB0, ( byte ) 0x4F, ( byte ) 0x71, ( byte ) 0x3B 
    };*/
    /*private byte[] zP;
    private byte[] xQ;
    private byte[] yQ;
    private byte[] zQ;
    */
    
    // Body na krivce
    private cBod P;
    private cBod Q;
    
    // Pomocny bod
    private cBod R;
    
    // Pomocna pole
    // T4 - T9 [DELKAPOLE]
    private cPolynom T4;
    private cPolynom T5;
//    private cPolynom T6;
    private cPolynom T7;
    private cPolynom T8;
    private cPolynom T9;
    
    // a[1+DELKAPOLE]
    private byte[] a;
    
    // c[2*DELKAPOLE]
    private byte[] c;
    /******************************************************************/
    
    
    /**
     * Installs this applet.
     *
     * @param bArray the array containing installation parameters
     * @param bOffset the starting offset in bArray
     * @param bLength the length in bytes of the parameter data in bArray
     */
    public static void install( byte[] bArray, short bOffset, byte bLength ) {
        ( new Applet1() ).register();
    }

    /**
     * Only this class's install method should create the applet object.
     */
    protected Applet1() { 
        // Inicializace bodu
        P = new cBod();
        Q = new cBod();
        R = new cBod();
        
        // Inicializace pomocnych polynomu
        T4 = new cPolynom();
        T5 = new cPolynom();
        T7 = new cPolynom();
        T8 = new cPolynom();
        T9 = new cPolynom();
        
        // A[1 + DELKAPOLE]
        a = JCSystem.makeTransientByteArray( DELKAPOLEplus1, JCSystem.CLEAR_ON_DESELECT );

        // C[2 * DELKAPOLE]
        c = JCSystem.makeTransientByteArray( DELKAPOLEkrat2, JCSystem.CLEAR_ON_DESELECT );
        
        prevedPoleNaPolynom( polynomJedna );
        prevedPoleNaPolynom( polynomNula );
        prevedPoleNaPolynom( ECa );
        prevedPoleNaPolynom( ECb );
        prevedPoleNaPolynom( ECc );
        
        register();
    }

    /**
     * Processes an incoming APDU.
     *
     * @see APDU
     * @param apdu the incoming APDU
     */
    public void process( APDU apdu ) {
        byte[] buffer = apdu.getBuffer();
        if ( selectingApplet() ) {
            ISOException.throwIt( ISO7816.SW_NO_ERROR );
        }
        
        byte trida = buffer[ISO7816.OFFSET_CLA];
        byte instrukce = buffer[ISO7816.OFFSET_INS];

        switch ( instrukce ) {
            case 0x01: // Scitani
                if ( parsujPrichoziData( apdu, buffer, B4 ) != 0 ) {
                    ISOException.throwIt( ISO7816.SW_DATA_INVALID );
                }
                
                P.prictiBod( Q );
                
                //Odesilani odpovedi test
                short le = apdu.setOutgoing();
                apdu.setOutgoingLength( (byte) (3*DELKAPOLE) );
                for (byte i = 0; i < DELKAPOLE; i++) {
                    buffer[i]             = P.X.hodnoty[DELKAPOLE-1-i];
                    buffer[DELKAPOLE+i]   = P.Y.hodnoty[DELKAPOLE-1-i];
                    buffer[2*DELKAPOLE+i] = P.Z.hodnoty[DELKAPOLE-1-i];
                }
                
                apdu.sendBytes( B0, (byte) (3*DELKAPOLE) );
                break;

            case 0x00: // Test
                short delka = apdu.setOutgoing();
                apdu.setOutgoingLength( delka );
                break;

            default:
                ISOException.throwIt( ISO7816.SW_INS_NOT_SUPPORTED );
        }
        // no error
//        JCSystem.requestObjectDeletion();
    }
    
    
    /**
     * Metoda ulozi oba body z prichoziho bufferu do tridnich promennych a
     * rozhodne, jestli jsou stejne
     *
     * @param apdu APDU Struktura s ulozenymi daty
     * @param buffer byte[] Buffer prichozich dat
     * @param pocetArgumentu byte Pocet parametru v datech bufferu
     * @return byte 1 pro dva ruzne body a 2 pro stejny bod (doubling)
     */
    public byte parsujPrichoziData( APDU apdu, byte[] buffer, byte pocetArgumentu ) {
        short delka = -1;

        try {
            delka = apdu.setIncomingAndReceive();
        } catch ( APDUException ex ) {
            // TODO #ERR
            return 1;
        }
        // TODO kontrola delka != 4*DELKAPOLE tak chyba
        try {
            Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA                 ), P.X.hodnoty, ( byte ) 0, DELKAPOLE );
            Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA +     DELKAPOLE ), P.Y.hodnoty, ( byte ) 0, DELKAPOLE );
            Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + 2 * DELKAPOLE ), Q.X.hodnoty, ( byte ) 0, DELKAPOLE );
            Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + 3 * DELKAPOLE ), Q.Y.hodnoty, ( byte ) 0, DELKAPOLE );
            // Osetreni zadavani bodu O
            //if ( jePoleNula( P.X.hodnoty ) && jePoleNula( P.Y.hodnoty ) ) {
            if ( P.X.jePolynomNula() && P.Y.jePolynomNula() ) {
                P.priradNekonecno();
            } else {
                prevedPoleNaPolynom( P.X.hodnoty );
                prevedPoleNaPolynom( P.Y.hodnoty );
                P.Z.priradPolynom( polynomJedna ); // zP[0] = B1;
            }
            //if ( jePoleNula( Q.X.hodnoty ) && jePoleNula( Q.Y.hodnoty ) ) {
            if ( Q.X.jePolynomNula() && Q.Y.jePolynomNula() ) {
                Q.priradNekonecno();
            } else {
                prevedPoleNaPolynom( Q.X.hodnoty );
                prevedPoleNaPolynom( Q.Y.hodnoty );
                Q.Z.priradPolynom( polynomJedna ); // zQ[0] = B1;
            }
        } catch ( ArrayIndexOutOfBoundsException ex ) {
            // TODO #ERR
            return 2;
        } catch ( NullPointerException ex ) {
            // TODO #ERR
            return 3;
        }

        return 0;
    }
    /******************************************************************/
    
    
    /************************* Operace s poli *************************/
    
    // Otoci poradi bajtu pole (human readable do computer readable formy)
    private static void prevedPoleNaPolynom( byte[] A ) {
        byte bajt, index;
        
        for (byte i = 0; i < (byte)(A.length/2); i++) {
            bajt = A[i];
            index = (byte) ( A.length-1-i );
            A[i] = A[index];
            A[index] = bajt;
        }
    }
    
    private static byte[] vytvorPrazdnePole( byte delka ) {
        return JCSystem.makeTransientByteArray( delka, JCSystem.CLEAR_ON_DESELECT );
    }
    // Vytvori prazdne pole delky DELKAPOLE
    private static byte[] vytvorPrazdnePole( ) {
        return vytvorPrazdnePole( DELKAPOLE );
    }
    
    private static void vynulujPole(byte[] a) {
         for( byte i=0 ; i<a.length ; i++) {
             a[i] = B0;
         }
     }
    
    // A <- B; nakopiruje pole B do A
    private static void zkopirujPole( byte[] A, final byte[] B, byte delka ) {
        Util.arrayCopyNonAtomic( B, B0, A, B0, delka );
        // for ( byte i = 0 ; i < delka ; i++ ) {
        //     A[i] = B[i];
        // }
    }
    
    // A <- B; nakopiruje pole B do A
    private static void zkopirujPole( byte[] A, final byte[] B ) {
        zkopirujPole( A, B, (byte) B.length );
    }
    
    // A <- A xor B; naxoruje pole B do A
    private static void prictiPole( byte[] A, final byte[] B, byte offset ) {
        for ( byte i = 0 ; i < B.length ; i++ ) {
            A[i+offset] ^= B[i];
        }
    }
    // Takto se v Jave definuje defaultni hodnota parametru (#PARAM)
    // #PARAM
    private static void prictiPole( byte[] A, final byte[] B ) {
        prictiPole( A, B, B0 );
    }
    
    // A<<1; posune pole A o jeden bit, vrati true/false, podle toho, jestli 
    // operace dopadla dobre, tedy, pokud nejvyssi bit byl 1, vrati false,
    // protoze doslo k prenosu / chybe
    private static boolean posunPoleDoleva( byte[] A ) {
        byte aktualniBit = 0x00;
        byte predeslyBit = 0x00;
        for ( byte i = 0 ; i < A.length ; i++ ) {
            aktualniBit  = (byte) ( (A[i] >> 7) & 0x01 );
            A[i]         = (byte) ( (A[i] << 1) & 0xFE );
            A[i]         = (byte) ( (A[i] | predeslyBit));
            predeslyBit  = aktualniBit;
        }
        // predeslyBit je stejny jako aktualniBit
        return ( predeslyBit == 0x00 );
    }
    
    // A_{indexBitu} == 1; vraci true/false na zaklade hodnoty daneho BITU v poli bytu
    private static boolean bitZPole( final byte[] A, final byte indexBitu ) {
        // adresace jednoho bitu z pole
        byte byteVPoli = (byte) (indexBitu / 8);
        byte bitVBytu  = (byte) (indexBitu % 8);
        // extrakce daneho bitu
        byte bit       = (byte) (( A[byteVPoli] >> bitVBytu ) & 0x01);
        return ( bit == 0x01 );
    }
    
    
    /**
     * @param a byte[] Prvni porovnavane pole
     * @param b byte[] Druhe porovnavane pole
     * @return byte 0-pole jsou stejna
     */
    public static boolean jsouPoleStejna( final byte[] a, final byte[] b ) {
        if ( a.length != b.length ) {
            return false;
        }
        for ( byte i = 0; i < a.length; i++ ) {
            if ( a[i] != b[i] ) {
                return false;
            }
        }
        return true;
    }

    public static boolean jePoleNula( final byte[] A ) {
        for ( byte i = 0 ; i < A.length ; i++ ) {
            if ( A[i] != B0 )
                return false;
        }
        return true;
    }
    /******************************************************************/
}

