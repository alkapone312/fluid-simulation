package pl.pwr.jkaplone.fluidsim.utils;

import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.Image;

public class TextureUtils {
    public static double getTexture2DSizeInMB(Texture2D texture) {
        Image image = texture.getImage();
        if (image == null) {
            return 0.0;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int bytesPerPixel = getBytesPerPixel(image.getFormat());

        // Podstawowy rozmiar warstwy bazowej (Mip 0)
        long totalBytes = (long) width * height * bytesPerPixel;

        // Skalowanie dla tekstur typu TextureCubeMap (które dziedziczą lub używają 6 warstw)
        // Jeśli to zwykłe 2D, traktujemy jako 1 warstwę
        int layers = image.getMultiSamples() > 1 ? image.getMultiSamples() : 1;
        totalBytes *= layers;

        // Zamiana na Megabajty
        double sizeInMB = (double) totalBytes / (1024.0 * 1024.0);

        // ⚠️ UWZGLĘDNIENIE MIPMAP
        // W teksturach 2D każda kolejna mipmapa jest 4x mniejsza od poprzedniej.
        // Suma nieskończonego ciągu (1 + 1/4 + 1/16 + ...) dąży do 1.333...
        // Jeśli tekstura ma wygenerowane mipmapy, zajmuje o ~33% więcej miejsca.
        if (image.hasMipmaps() || isMipmappedFilter(texture.getMinFilter())) {
            sizeInMB *= 1.3333;
        }

        return sizeInMB;
    }

    private static boolean isMipmappedFilter(Texture.MinFilter filter) {
        return filter == Texture.MinFilter.Trilinear ||
            filter == Texture.MinFilter.BilinearNoMipMaps ||
            filter == Texture.MinFilter.NearestLinearMipMap ||
            filter == Texture.MinFilter.NearestNearestMipMap;
    }


    public static double getTexture3DSizeInMB(Texture3D texture) {
        Image image = texture.getImage();
        if (image == null) {
            return 0.0;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int depth = image.getDepth(); // Number of 3D layers

        // Fallback to 1 if depth is not set properly in the image metadata
        if (depth <= 0) depth = 1;

        int bytesPerPixel = getBytesPerPixel(image.getFormat());

        // Total bytes
        long totalBytes = (long) width * height * depth * bytesPerPixel;

        // Convert to Megabytes (1024 * 1024)
        return (double) totalBytes / (1024.0 * 1024.0);
    }

    private static int getBytesPerPixel(Image.Format format) {
        switch (format) {
            case Alpha8:
            case Luminance8:
            case RGB8:
                return 3; // RGB (3 bytes)
            case RGBA8:
                return 4; // RGBA (4 bytes - Most Common)
            case R16F:
                return 2; // Half-precision float (16-bit)
            case RG16F:
                return 4;
            case RGB16F:
                return 6;
            case RGBA16F:
                return 8; // 4 channels x 2 bytes
            case R32F:
                return 4; // Full-precision float (32-bit)
            case RGBA32F:
                return 16; // 4 channels x 4 bytes
            case Depth:
                return 4; // Usually 32-bit depth buffer
            default:
                // If it's a compressed format (e.g., DXT1, DXT5), the math changes.
                // For uncompressed formats, default to 4 bytes as a safe guess.
                return 4;
        }
    }
}