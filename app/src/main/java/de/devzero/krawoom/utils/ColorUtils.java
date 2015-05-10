package de.devzero.krawoom.utils;

import org.andengine.util.adt.color.Color;

public class ColorUtils {

    // http://www.cs.rit.edu/~ncs/color/t_convert.html
    // r,g,b values are from 0 to 1
    // h = [0,360], s = [0,1], v = [0,1]
    public static Color HSVtoRGB(float h, float s, float v )
    {
        float r,g,b;
        int i;
        float f, p, q, t;
        if( s == 0 ) {
            // achromatic (grey)
            r = g = b = v;
            return new Color(r,g,b);
        }
        h /= 60;			// sector 0 to 5
        i = (int)h;
        f = h - i;			// factorial part of h
        p = v * ( 1 - s );
        q = v * ( 1 - s * f );
        t = v * ( 1 - s * ( 1 - f ) );
        switch( i ) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:		// case 5:
                r = v;
                g = p;
                b = q;
                break;
        }
        return new Color(r,g,b);
    }
}
