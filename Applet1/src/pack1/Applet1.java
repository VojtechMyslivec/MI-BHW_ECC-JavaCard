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

    // Konstanty
    private static final byte   DELKAPOLE      = (byte)( 10 );
    private static final byte   DELKAPOLEkrat2 = (byte)( 2 * DELKAPOLE );
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
    
    private byte[] xP;
    private byte[] yP;
    private byte[] zP;
    private byte[] xQ;
    private byte[] yQ;
    private byte[] zQ;
//     cBinarniTeleso xBodA;
//     cBinarniTeleso yBodA;
//     cBinarniTeleso zBodA;
//     cBinarniTeleso xBodB;
//     cBinarniTeleso yBodB;
//     cBinarniTeleso zBodB;
    short delkaArgumentu = -1;

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
        xP = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        yP = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        zP = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        xQ = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        yQ = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        zQ = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        
        //xBodA = new cBinarniTeleso();
        //yBodA = new cBinarniTeleso();
        //zBodA = new cBinarniTeleso();
        //xBodB = new cBinarniTeleso();
        //yBodB = new cBinarniTeleso();
        //zBodB = new cBinarniTeleso();
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
                sectiBody();

                //Odesilani odpovedi test
                short le = apdu.setOutgoing();
                apdu.setOutgoingLength( ( byte ) 3 );
                buffer[0] = ( byte ) le;
                buffer[1] = ( byte ) 0xFF;
                buffer[2] = ( byte ) 0x33;
                apdu.sendBytes( ( byte ) 0, ( byte ) 3 );
                break;

            case 0x00: // Test
                short delka = apdu.setOutgoing();
                apdu.setOutgoingLength( delka );
                break;

            default:
                ISOException.throwIt( ISO7816.SW_INS_NOT_SUPPORTED );
        }
        // no error
        JCSystem.requestObjectDeletion();
    }

    // P <- 2*P; algoritmus dle [4], A.10.6
    // ( v Q je totozny bod s P )
    public void zdvojBod( ) {
        if ( jePoleNula( xP ) || jePoleNula( zP ) ) { // 5.
            // Vysledek bude bod v nekonecnu
            priradPolynom( xP, polynomJedna );
            priradPolynom( yP, polynomJedna );
            priradPolynom( zP, polynomNula );
            return;
        }

        byte[] T1 = vytvorKopiiPolynomu( xP );   // 1.               
        priradPolynom( xP, ECc );      // 4.

        prinasobPolynom( yP, zP );     // 6.
        umocniPolynom( zP );           // 7.
        prinasobPolynom( xP, zP );     // 8.
        prinasobPolynom( zP, T1 );     // 9. a 23. // zP

        prictiPolynom( yP, zP );       // 10.
        prictiPolynom( xP, T1 );       // 11.
        umocniPolynom( xP );           // 12.
        umocniPolynom( xP );           // 13. a 21. // xP

        umocniPolynom( T1 );           // 14.
        prictiPolynom( yP, T1 );       // 15.
        prinasobPolynom( yP, xP );     // 16.
        umocniPolynom( T1 );           // 17.
        prinasobPolynom( T1, zP );     // 18.
        prictiPolynom( yP, T1 );       // 19. a 22. // yP

        // TODO
        T1.requestObjectDeletion();
    }

    // P <- P + Q; algoritmus dle [4], A.10.7
    public void sectiBody( ) {
        // xP = T1
        // yP = T2
        // zP = T3
        // xQ = T4
        // yQ = T5
        // zQ = T6
        byte[] T7 = vytvorKopiiPolynomu( zQ );

        if ( ! jsouPoleStejna( zQ, polynomJedna ) ) { // 7.
            umocniPolynom( T7 );
            prinasobPolynom( xP, T7 );              //vypocte U0
            prinasobPolynom( T7, zQ );
            prinasobPolynom( T2, T7 );              // vypocte S0            
        }
        priradPolynom( T7, zP );
        umocniPolynom( T7 );        // 8.

        byte[] T8 = vytvorKopiiPolynomu( T7 );    // 9a.
        prinasobPolynom( T8, xQ );  // 9.   // vypocte U1
        prictiPolynom( xP, T8 );    // 10.  // vypocte W
        prinasobPolynom( T7, zP );  // 11.

        priradPolynom( T8, T7 );    // 12a.
        prinasobPolynom( T8, T5 );  // 12.  // vypocte S1
        prictiPolynom( yP, T8 );    // 13.     // vypocte R
        
        if ( jePoleNula( xP ) ) {   // 14.
            priradPolynom( zP, polynomNula );
            if ( ! jePoleNula( yP ) ) {
                priradPolynom( xP, polynomJedna );
                priradPolynom( yP, polynomJedna );
            }
            JCSystem.requestObjectDeletion(); // T7, T8
            return;
        }

        prinasobPolynom( xQ, yP );  // 15.
        prinasobPolynom( zP, xP );  // 16.  // vypocte L a Z2 pokud Z1==1
        prinasobPolynom( yQ, zP );  // 17.
        
        prictiPolynom( xQ, yQ );    // 18.  // vypocte V
        priradPolynom( yQ, zP );    // 19a.
        umocniPolynom( yQ );        // 19.
        priradPolynom( T7, yQ );    // 20a.
        prinasobPolynom( T7, xQ );  // 20.

        if ( ! jsouPoleStejna( zQ, polynomJedna ) ) {   //21.
            prinasobPolynom( zP, zQ );  // vypocte Z2 pokud z1!=1
        }
        
        priradPolynom( xQ, zP );    // 22a.
        prictiPolynom( xQ, yP );    // 22.  // vycpote T

        prinasobPolynom( yP, xQ );  // 23.
        priradPolynom( yQ, xP );    // 24a.
        umocniPolynom( yQ );        // 24
        prinasobPolynom( xP, yQ );  // 25

        if ( ! jePoleNula( ECa ) ) { // 6. 26.
            byte[] T9 = vytvorKopiiPolynomu( ECa ); // 6.
            priradPolynom( T8, zP );    // 26.
            umocniPolynom( T8 );
            prinasobPolynom( T9, T8 );
            prictiPolynom( xP, T9 );
            // Smazat T9
            JCSystem.requestObjectDeletion(); //T9
        }
        

        prictiPolynom( xP, yP );    // 27.
        prinasobPolynom( xQ, xP );  // 28.
        priradPolynom( yP, xQ );    // 29a.
        prictiPolynom( yP, T7 );    // 29.  // vypocte Y2

        JCSystem.requestObjectDeletion();

        return;
    }

    public void nasobBod( byte skalar ) {

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
        delkaArgumentu = ( byte ) ( delka / pocetArgumentu );

        try {
            // Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + delkaArgumentu ), xBodA.hodnoty, ( byte ) 0, delkaArgumentu ); + alokace
            // Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + delkaArgumentu ), yBodA, ( byte ) 0, delkaArgumentu );
            // Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + 2 * delkaArgumentu ), xBodB, ( byte ) 0, delkaArgumentu );
            // Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + 3 * delkaArgumentu ), yBodB, ( byte ) 0, delkaArgumentu );
            // zBodA[0] = B1;
            // zBodB[0] = B1;

            byte[] tmp = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );

            Util.arrayCopyNonAtomic( buffer, ISO7816.OFFSET_CDATA, tmp, ( byte ) 0, delkaArgumentu );
            xP.inicializace( tmp );

            Util.arrayCopyNonAtomic( buffer, (short) ( ISO7816.OFFSET_CDATA+(1*delkaArgumentu) ), tmp, ( short ) 0, delkaArgumentu );
            yP.inicializace( tmp );

            Util.arrayCopyNonAtomic( buffer, (short) ( ISO7816.OFFSET_CDATA+(2*delkaArgumentu) ), tmp, ( short ) 0, delkaArgumentu );
            xQ.inicializace( tmp );

            Util.arrayCopyNonAtomic( buffer, (short) ( ISO7816.OFFSET_CDATA+(3*delkaArgumentu) ), tmp, ( short ) 0, delkaArgumentu );
            yQ.inicializace( tmp );

            Util.arrayFillNonAtomic( tmp, S0, (short)tmp.length, B0);
            tmp[0] = B1;
            zBodA.inicializace( tmp );
            zBodB.inicializace( tmp );
        } catch ( ArrayIndexOutOfBoundsException ex ) {
            // TODO #ERR
            return 2;
        } catch ( NullPointerException ex ) {
            // TODO #ERR
            return 3;
        }

        return 0;
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
            if ( a[i] != B0 )
                return false;
        }
        return true;
    }

    private static byte[] vytvorPrazdnePole( byte delka = DELKAPOLE ) {
        return JCSystem.makeTransientByteArray( delka, JCSystem.CLEAR_ON_RESET );
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

    // A <- B; nakopiruje pole B do A
    private static void zkopirujPole( byte[] A, final byte[] B ) {
        Util.arrayCopyNonAtomic( B, B0, A, B0, A.length );
        // for ( byte i = 0 ; i < delka ; i++ ) {
        //     A[i] = B[i];
        // }
    }

    // A <- A xor B; naxoruje pole B do A
    private static void prictiPole( byte[] A, final byte[] B, byte offset = 0 ) {
        for ( byte i = 0 ; i < B.length ; i++ ) {
            A[i+offset] ^= B[i];
        }
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

    // A <- B mod P; inspirace z [3], alg. 4
    // Redukuje pole B max do delky 2*DELKAPOLE, ale nejvyssi bit 
    // dvojnasobneho pole musi byt 0, jinak neredukuje vse.
    // To by nemelo nastat ani po nasobeni ani po mocneni.
    // Vraci false v pripade, ze je tento bit 1 -- tedy v pripade spatneho
    // vysledku.
    // Jinak true.
    private static boolean redukujDvojnasobnyPolynom( byte[] A, final byte[] B ) {
        byte horniCast, spodniCast, bajt, index;
        byte[] C = vytvorPrazdnePole( DELKAPOLEkrat2 );
        zkopirujPole( C, B );

        for ( index = DELKAPOLEkrat2 - 1 ; index >= DELKAPOLE; index-- ) {
            horniCast  = ( C[index]   << 1 ) & 0xFE;
            spodniCast = ( C[index-1] >> 7 ) & 0x01;
            bajt       = horniCast ^ spodniCast;
            // pro x^9 ..........................
            // horni bit
            horniCast  = (byte) ( (bajt >> 7) & 0x01 );
            // sedm spodnich bitu
            spodniCast = (byte) ( (bajt << 1) & 0xFE );
            // horni  cast -- xor k hodnotam o 8 bytu nize,
            C[index-8] ^= horniCast;
            // spodni cast -- xor k hodnotam o 9 bytu nize;
            C[index-9] ^= spodniCast;

            // pro x^0 ..........................
            // zde je to jednoduche
            C[index-10] ^= bajt;
        }
        index      = DELKAPOLEkrat2;
        spodniCast = ( C[index-1] >> 7 ) & 0x01;

        // vysledek nakopiruje do A
        zkopirujPole( A, C );
        // TODO
        C.requestObjectDeletion();

        return ( spodniCast == 0x00 );
    }
    
//     public void inicializace( byte[] polynom ) {
//         hodnoty = vytvorPrazdnePole( DELKAPOLE );
//         for ( short i = 0 ; i < DELKAPOLE ; i++ ) { 
//             hodnoty[i] = polynom[i];
//         }
//     }

//     public void destruct() {
//         // TODO
//         JCSystem.requestObjectDeletion();
//     }

    // A <- B
    public static boolean priradPolynom( byte[] A, final byte[] B ) {
        zkopirujPole( A, B );
        return true;
    }

    // kopie A
    public static byte[] vytvorKopiiPolynomu( final byte[] A ) {
        byte[] B = vytvorPrazdnePole();
        priradPolynom( B, A )
        return B;
    }

    // A <- A xor B
    public static boolean prictiPolynom( byte[] A, final byte[] B ) {
        prictiPole( A, B );
        return true;
    }

    // A <- A * B mod P
    // Comb metoda, dle [3], alg. 2 
    public static boolean prinasobPolynom( byte[] A, final byte[] B ) {
        // aktualni hodnoty pole, v prubehu vypoctu se posouvaji max o 1 B
        byte[] a = vytvorPrazdnePole( (byte)( DELKAPOLE + 1 ) );
        zkopirujPole( a, A );
        // jen ukazatel
        byte[] b = B;
        // v c postupne vznika vysledek
        byte[] c = vytvorPrazdnePole( DELKAPOLEkrat2 );

        // nasobeni ----------------------------------------
        // cyklus pres bity
        for ( byte j = 0 ; j < 8 ; j++ ) {
            // cyklus pres bajty
            for ( byte i = 0 ; i < DELKAPOLE ; i++ ) {
                if ( bitZPole( b, (byte) ( i*8 + j ) )
                    prictiPole( c, a, i )
            }
            if ( ! posunPoleDoleva( a ) ) {
                // nesmi nasat, ze by nejaky bit vypadl. Znamenalo by to,
                // ze se nasobilo necim vice nez x^79
                return false;
            }
        }
        // redukce -----------------------------------------
        if ( ! redukujDvojnasobnyPolynom( hodnoty, c ) ) {
            // Nemelo by nastat. Znamenalo by to, ze byl 
            // nejvyssi bit nastaven na 1
            return false;
        }

        // TODO
        a.requestObjectDeletion();
        c.requestObjectDeletion();

        return true;
    }

    // A <- A^2 mod P
    // expanze -- prolozeni nulami -- a nasledna redukce, dle [2], 
    // Squaring over GF(2^m) with polynomial basis
    public static boolean umocniPolynom( byte[] A ) {
        byte[] C = vytvorPrazdnePole( DELKAPOLEkrat2 );
        byte   nibble;
        // expanze -----------------------------------------
        for ( byte i = DELKAPOLE-1 ; i >= 0 ; i-- ) {
            // spodni nibble
            nibble   = ( A[i]      ) & 0x0F;
            C[2*i]   = expanze[nibble];
            // horni nibble
            nibble   = ( A[i] >> 4 ) & 0x0F;
            C[2*i+1] = expanze[nibble];
        }

        // redukce -----------------------------------------
        if ( ! redukujDvojnasobnyPolynom( A, C ) ) {
            // Nemelo by nastat. Znamenalo by to, ze byl 
            // nejvyssi bit nastaven na 1
            return false;
        }

        // TODO
        C.requestObjectDeletion();
        return true;
    }
    
}

