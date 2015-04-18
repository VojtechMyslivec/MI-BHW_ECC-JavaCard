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
 *     [3]     Merchan J. G., Güneysu T., Kumar S., Paar C., Pelzl J.
 *             Efficient Software Implementation of Finite Fields with 
 *             Applications to Cryptography v Acta Applicandae Mathematicae: 
 *             An International Survey Journal on Applying Mathematics and
 *             Mathematical Applications, Volume 93, Numbers 1-3, pp. 3-32,
 *             September 2006. Ruhr-Universitat Bochum, 2006.
 *             <http://www.emsec.rub.de/research/publications/efficient-software-implementation-finite-fields-ap/>
 *
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
            hodnoty = vytvorPrazdnePole( DELKAPOLE );
            for ( short i = 0 ; i < DELKAPOLE ; i++ ) { 
                hodnoty[i] = polynom[i];
            }
        }

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

        // Comb metoda, dle [3], alg. 2 
        public boolean prinasob( cBinarniTeleso B ) {
            // aktualni hodnoty pole, v prubehu vypoctu se posouvaji max o 1 B
            byte[] a = vytvorPrazdnePole( (byte)( DELKAPOLE + 1 ) );
            zkopirujPole( a, hodnoty );
            // jen ukazatel
            byte[] b = B.hodnoty;
            // v c postupne vznika vysledek
            byte[] c = vytvorPrazdnePole( DELKAPOLEkrat2 );

            // nasobeni ----------------------------------------
            // cyklus pres bity
            for ( byte j = 0 ; j < 8 ; j++ ) {
                // cyklus pres bajty
                for ( byte i = 0 ; i < DELKAPOLE ; i++ ) {
                    if ( bitZPole( b, (byte) ( i*8 + j ) )
                        prictiPole( c, a, (byte)( DELKAPOLE + 1 ), i )
                }
                if ( ! posunPoleDoleva( a, (byte)( DELKAPOLE + 1 ) ) ) {
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
            delete [] a;
            delete [] c;

            return true;
        }

        // expanze -- prolozeni nulami -- a nasledna redukce, dle [2], 
        // Squaring over GF(2^m) with polynomial basis
        // TODO kontrola
        public boolean umocni( ) {
            byte[] c = vytvorPrazdnePole( DELKAPOLEkrat2 );
            byte   nibble;
            // expanze -----------------------------------------
            for ( byte i = DELKAPOLE-1 ; i >= 0 ; i-- ) {
                // spodni nibble
                nibble    = hodnoty[i] & 0x0F;
                c[i]   = expanze[nibble];
                // horni nibble
                nibble    = (hodnoty[i]>>4) & 0x0F;
                c[i+1] = expanze[nibble];
            }

            // redukce -----------------------------------------
            if ( ! redukujDvojnasobnyPolynom( hodnoty, c ) ) {
                // Nemelo by nastat. Znamenalo by to, ze byl 
                // nejvyssi bit nastaven na 1
                return false;
            }

            // TODO
            delete [] c;
            return true;
        }


    };
    // --------- end of class cBinarniTeleso ---------


    // Konstanty
    private static final byte   DELKAPOLE      = (byte)( 10 );
    private static final byte   DELKAPOLEkrat2 = (byte)( 2 * DELKAPOLE );
    private static final byte   B0 = 0;
    private static final byte   B1 = 1;
    private static final byte   B2 = 2;
    private static final byte   B4 = 4;
    private static final short  S0 = 0;
    private static final short  DOLNICH8BITU = 0x00FF; // smazat?
    private static final byte[] ECa = { 
       ( byte ) 0x4A, ( byte ) 0x2E, ( byte ) 0x38, ( byte ) 0xA8, ( byte ) 0xF6,
       ( byte ) 0x6D, ( byte ) 0x7F, ( byte ) 0x4C, ( byte ) 0x38, ( byte ) 0x5F 
    };
    private static final byte[] ECb = { 
       ( byte ) 0x2C, ( byte ) 0x0B, ( byte ) 0xB3, ( byte ) 0x1C, ( byte ) 0x6B,
       ( byte ) 0xEC, ( byte ) 0xC0, ( byte ) 0x3D, ( byte ) 0x68, ( byte ) 0xA7 
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
        // xBodA = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        // yBodA = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        // zBodA = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        // xBodB = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        // yBodB = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
        // zBodB = JCSystem.makeTransientByteArray( DELKAPOLE, JCSystem.CLEAR_ON_RESET );
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

    // TODO ?
    public void sectiBody() {
        //xBodA.pricti( yBodA );
        xBodA.prinasob( yBodA );
    }

    // TODO smazat?
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

    // TODO smazat?
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

    // TODO smazat?
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
            // Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + delkaArgumentu ), xBodA.hodnoty, ( byte ) 0, delkaArgumentu ); + alokace
            // Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + delkaArgumentu ), yBodA, ( byte ) 0, delkaArgumentu );
            // Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + 2 * delkaArgumentu ), xBodB, ( byte ) 0, delkaArgumentu );
            // Util.arrayCopyNonAtomic( buffer, ( byte ) ( ISO7816.OFFSET_CDATA + 3 * delkaArgumentu ), yBodB, ( byte ) 0, delkaArgumentu );
            // zBodA[0] = B1;
            // zBodB[0] = B1;

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

    private static byte[] vytvorPrazdnePole( byte delka = DELKAPOLE ) {
        return JCSystem.makeTransientByteArray( delka, JCSystem.CLEAR_ON_RESET );
    }

    // A<<1; posune pole A o jeden bit, vrati true/false, podle toho, jestli 
    // operace dopadla dobre, tedy, pokud nejvyssi bit byl 1, vrati false,
    // protoze doslo k prenosu / chybe
    private static boolean posunPoleDoleva( byte[] A, byte delka = DELKAPOLE ) {
        byte aktualniBit = 0x00;
        byte predeslyBit = 0x00;
        for ( byte i = 0 ; i < delka ; i++ ) {
            aktualniBit  = (byte) ( (A[i] >> 7) & 0x01 );
            A[i]         = (byte) ( (A[i] << 1) & 0xFE );
            A[i]         = (byte) ( (A[i] | predeslyBit));
            predeslyBit  = aktualniBit;
        }
        // predeslyBit je stejny jako aktualniBit
        return ( predeslyBit == 0x00 );
    }

    // A <- B; nakopiruje pole B do A
    private static void zkopirujPole( byte[] A, final byte[] B, byte delka = DELKAPOLE ) {
        for ( short i = 0 ; i < delka ; i++ ) {
            A[i] = B[i];
        }
    }

    // A <- A xor B; naxoruje pole B do A
    private static void prictiPole( byte[] A, final byte[] B, byte delka = DELKAPOLE, byte offset = 0 ) {
        for ( short i = 0 ; i < delka ; i++ ) {
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
        zkopirujPole( C, B, DELKAPOLEkrat2 );

        for ( index = DELKAPOLEkrat2 - 1 ; index >= DELKAPOLE ; index-- ) {
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
        // spodniCast = ( B[index-1] >> 7 ) & 0x01;
        index      = DELKAPOLEkrat2 - 1;
        spodniCast = ( C[index] >> 7 ) & 0x01;

        // vysledek nakopiruje do A
        zkopirujPole( A, C );
        // TODO
        delete [] C;

        return ( spodniCast == 0x00 );
    }
}

