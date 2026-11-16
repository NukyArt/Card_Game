package com.myjavacardgame.presentation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/** Creates independently owned Cyrillic UI fonts from the bundled OFL font. */
public final class UiFonts {
    private UiFonts() { }

    /** Creates a font after graphics initialization; the caller must dispose it. */
    public static BitmapFont create(int size) {
        var generator = new FreeTypeFontGenerator(Gdx.files.internal("assets/fonts/NotoSans-Regular.ttf"));
        try {
            var parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = size;
            var characters = new StringBuilder(FreeTypeFontGenerator.DEFAULT_CHARS);
            for (char character = 'Ѐ'; character <= 'ӿ'; character++) characters.append(character);
            parameter.characters = characters.append("—−№→").toString();
            return generator.generateFont(parameter);
        } finally { generator.dispose(); }
    }
}
