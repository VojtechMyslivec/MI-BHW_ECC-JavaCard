/**
 * Autori
 *  Vojtech Myslivec
 *  Zdenek Novy
 */

package pack1;

import javacard.framework.*;

/**
 *
 * @author novyzde3, myslivo1, bucekj
 */
public class Applet1 extends Applet {
    
    public class cBinarniTeleso {
        private byte[] hodnoty;
        
        public void inicializace( byte[] polynom ) {
           hodnoty = inicializujPole( hodnoty );
           for ( short i = 0 ; i < DELKAPOLE ; i++ ) { 
              hodnoty[i] = polynom[i];
           }
        }
        
        /*protected cBinarniTeleso( byte[] polynom ) {
           inicializujPole( hodnoty );
           for ( short i = 0 ; i < DELKAPOLE ; i++ ) { 
              hodnoty[i] = polynom[i];
           }
        }*/
        
        //public ~cBinarniTeleso() {
        //   // TODO ?
        //   delete [] hodnoty;
        //}
        public void destruct() {
            // TODO
            JCSystem.requestObjectDeletion();
        }
        
        public boolean pricti( cBinarniTeleso B ) {
           for ( short i = 0 ; i < DELKAPOLE ; i++ ) { 
               hodnoty[i] ^= B.hodnoty[i];
           }
           return true;
        }
        
        // Double and add, MSB viz Paar, alg. 1
        public boolean prinasob( cBinarniTeleso B ) {
           // pomocne pole -- polynom 0 // TODO mozna se hodi jako private static final
           byte[] nula = null;
           // v u postupne vznika vysledek
           byte[] u = null;
           // v je ukazatel na pole, ktere se k u pricita
           byte[] v;
           
           nula = inicializujPole( nula );
           u = inicializujPole( u );
           byte[] x = hodnoty;
           byte[] y = B.hodnoty;
           
            // for ( byte indexBitu2 = DELKAPOLE*8 ; indexBitu2 >= 0 ; indexBitu2 -- ) {
           for ( byte indexBitu2 = DELKAPOLE*8-1 ; indexBitu2 >= 0 ; indexBitu2 -- ) {
               // v <- 2*v mod p
              if ( posunPoleDoleva( u ) ) 
                 redukuj( u );

              // v <- x[indexBitu] * y
              if ( bitZPole( x, indexBitu2 ) )
                 v = y;
              else
                 v = nula;

              // v <- x[indexBitu] * y
              prictiPole( u, v );
           }

           //delete [] nula;
           //delete [] hodnoty;
           JCSystem.requestObjectDeletion();
           hodnoty = u;
           return true;
        }

        public boolean umocni( cBinarniTeleso B ) {
           // TODO
           return true;
        }
        
        //private static void inicializujPole( byte[] A ) {
        //   A = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        //}
        
        // A <- A xor B; naxoruje pole B do A
        private void prictiPole( byte[] A, final byte[] B ) {
            for ( short i = 0 ; i < DELKAPOLE ; i++ ) {
                A[i] ^= B[i];
            }
        }
    
    };
    // --------- end of class cBinarniTeleso ---------
    
    
    // Konstanty
    private static final short  DELKAPOLE = 10; //musi byt presne - nic navic
    private static final byte   B0 = 0;
    private static final byte   B1 = 1;
    private static final byte   B2 = 2;
    private static final byte   B4 = 4;
    private static final short  S0 = 0;
    private static final short  DOLNICH8BITU = 0x00FF;
    private static final byte[] ECa = { ( byte ) 0x4A, ( byte ) 0x2E, ( byte ) 0x38, ( byte ) 0xA8, ( byte ) 0xF6,
        ( byte ) 0x6D, ( byte ) 0x7F, ( byte ) 0x4C, ( byte ) 0x38, ( byte ) 0x5F };
    private static final byte[] ECb = { ( byte ) 0x2C, ( byte ) 0x0B, ( byte ) 0xB3, ( byte ) 0x1C, ( byte ) 0x6B,
        ( byte ) 0xEC, ( byte ) 0xC0, ( byte ) 0x3D, ( byte ) 0x68, ( byte ) 0xA7 };

    
    cBinarniTeleso xBodA;
    cBinarniTeleso yBodA;
    cBinarniTeleso zBodA;
    cBinarniTeleso xBodB;
    cBinarniTeleso yBodB;
    cBinarniTeleso zBodB;
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
        /*xBodA = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        yBodA = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        zBodA = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        xBodB = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        yBodB = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        zBodB = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );*/
        xBodA = new cBinarniTeleso();
        yBodA = new cBinarniTeleso();
        zBodA = new cBinarniTeleso();
        xBodB = new cBinarniTeleso();
        yBodB = new cBinarniTeleso();
        zBodB = new cBinarniTeleso();
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

    public void sectiBody() {
        //xBodA.pricti( yBodA );
        xBodA.prinasob( yBodA );
    }

    public byte[] nasob( byte[] opA, byte[] opB ) {
        // Vysledne pole (implicitne vynulovane)
        byte[] vysledek = JCSystem.makeTransientByteArray( ( short ) ( 2 * DELKAPOLE ), JCSystem.CLEAR_ON_RESET );
        byte[] pomocnePole = JCSystem.makeTransientByteArray( ( short ) ( 2 * DELKAPOLE ), JCSystem.CLEAR_ON_RESET );
        short pomocny = 0;

        for ( short i = ( short ) ( delkaArgumentu - 1 ); i >= 0; i-- ) {
            pomocny = ( short ) ( ( vysledek[i] * 2 ) );
            pomocnePole = nasobSkalarem( opB[i], opA );
        }
        return vysledek;
    }

    public byte[] nasobSkalarem( byte skalar, byte[] opA ) {
        byte [] pomocnePole = JCSystem.makeTransientByteArray( ( short ) ( opA.length + 1 ), JCSystem.CLEAR_ON_RESET );
        short tmp = 0;
        byte prenos = 0;

        for ( short i=0 ; i<opA.length ; i++ ) {
            tmp = opA[i];
            tmp = (short) (tmp & DOLNICH8BITU); // odstraneni znamenkoveho rozsireni
            tmp = (short) (tmp * skalar);
            pomocnePole[i] = ( byte ) ( tmp & DOLNICH8BITU );
            pomocnePole[i] += prenos;
            tmp >>= 8;
            prenos = (byte) tmp;
        }
        // Posledni prenos max 1 bytu
        pomocnePole[opA.length] = prenos;

        return pomocnePole;
    }

    public byte[] square( byte[] opA ) {
        byte[] tmp = JCSystem.makeTransientByteArray( ( short ) ( 2 * DELKAPOLE ), JCSystem.CLEAR_ON_RESET );
        for ( short i = 0; i < DELKAPOLE; i++ ) {

        }

        return tmp;
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
            /*Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + delkaArgumentu ), xBodA.hodnoty, ( byte ) 0, delkaArgumentu ); + alokace
            Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + delkaArgumentu ), yBodA, ( byte ) 0, delkaArgumentu );
            Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + 2 * delkaArgumentu ), xBodB, ( byte ) 0, delkaArgumentu );
            Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + 3 * delkaArgumentu ), yBodB, ( byte ) 0, delkaArgumentu );*/
            /*zBodA[0] = B1;
            zBodB[0] = B1;*/
            
            byte[] tmp = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
            
            Util.arrayCopyNonAtomic( buffer, ISO7816.OFFSET_CDATA, tmp, ( byte ) 0, delkaArgumentu );
            xBodA.inicializace( tmp );
            
            Util.arrayCopyNonAtomic( buffer, (short) ( ISO7816.OFFSET_CDATA+(1*delkaArgumentu) ), tmp, ( short ) 0, delkaArgumentu );
            yBodA.inicializace( tmp );
            
            Util.arrayCopyNonAtomic( buffer, (short) ( ISO7816.OFFSET_CDATA+(2*delkaArgumentu) ), tmp, ( short ) 0, delkaArgumentu );
            xBodB.inicializace( tmp );
            
            Util.arrayCopyNonAtomic( buffer, (short) ( ISO7816.OFFSET_CDATA+(3*delkaArgumentu) ), tmp, ( short ) 0, delkaArgumentu );
            yBodB.inicializace( tmp );
            
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
     *
     * @param a byte[] Prvni porovnavane pole
     * @param b byte[] Druhe porovnavane pole
     * @return byte 0-pole jsou stejna
     */
    public byte jsouPoleStejna( byte[] a, byte[] b ) {
        if ( a.length != b.length ) {
            return 1;
        }

        for ( short i = 0; i < a.length; i++ ) {
            if ( a[i] != b[i] ) {
                return 1;
            }
        }
        return 0;
    }
    
    private static byte[] inicializujPole( byte[] A ) {
        A = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        return A;
    }
    
    // A<<1 posune pole A o jeden bit, vrati true/false podle bitu, co vypadl
    private static boolean posunPoleDoleva( byte[] A ) {
           byte aktualniBit = 0;
           byte predeslyBit = 0;
           for ( short i = 0 ; i < DELKAPOLE ; i++ ) {
              aktualniBit   = (byte) ( (0x80 & A[i]) >> 7  );
              A[i]          = (byte) ( (A[i] << 1) & 0xFE  );
              A[i]          = (byte) ( (A[i] | predeslyBit));
              predeslyBit = aktualniBit;
           }
           return ( aktualniBit == 0x01 );
    }
    
    // A <- B; nakopiruje pole B do A
    private static void zkopirujPole( byte[] A, final byte[] B ) {
        for ( short i = 0 ; i < DELKAPOLE ; i++ ) {
            A[i] = B[i];
        }
    }
        
    // A[indexBitu] == 1; vraci true/false na zaklade hodnoty daneho BITU v poli bytu
    private static boolean bitZPole( final byte[] A, final byte indexBitu ) {
        // adresace jednoho bitu z pole
        byte byteVPoli = (byte) (indexBitu / 8);
        byte bitVBytu  = (byte) (indexBitu % 8);
        // extrakce daneho bitu
        byte bit       = (byte) (( A[byteVPoli] >> bitVBytu ) & 0x01);
        return ( bit == 0x01 );
    }
    
    // A + x^n; pricte k poli A polynom kongruentni s x^n 
    private static void redukuj( byte[] A ) {
       // TODO x^n ~ x^9 + 1
       // TODO (byte) ?
       // TODO je to konstanta =>
       // A xor x^9
       A[1] ^= 0x02;
       // A xor 1 = x^0
       A[0] ^= 0x01;
    }
}
