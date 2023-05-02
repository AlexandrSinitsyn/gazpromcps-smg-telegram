package gazpromcps.smg;

import lombok.SneakyThrows;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.function.Function;

public class XlsxTest {

    private static String toHex(final int b) {
        final Function<Integer, String> cast = i -> switch (i) {
            case 10 -> "A";
            case 11 -> "B";
            case 12 -> "C";
            case 13 -> "D";
            case 14 -> "E";
            case 15 -> "F";
            default -> String.valueOf(i);
        };

        return cast.apply(b & 0xF0 / 16) + cast.apply(b & 0x0F);
    }

    private static String getFillColorHex(final Cell cell) {
        if (cell == null) {
            return null;
        }

        final CellStyle cellStyle = cell.getCellStyle();
        final Color color = cellStyle.getFillForegroundColorColor();

        if (color == null) {
            return null;
        }

        final Function<Number, String> cast = b -> toHex(b.byteValue() & 0xFF);

        if (color instanceof final XSSFColor xssfColor) {
            byte[] argb = xssfColor.getARGB();

            if (xssfColor.hasTint()) {
                byte[] rgb = xssfColor.getRGBWithTint();
                return cast.apply(argb[0]) + cast.apply(rgb[0]) + cast.apply(rgb[1]) + cast.apply(rgb[2]);
            } else {
                return cast.apply(argb[0]) + cast.apply(argb[1]) + cast.apply(argb[2]) + cast.apply(argb[3]);
            }
        } else if (color instanceof final HSSFColor hssfColor) {
            short[] rgb = hssfColor.getTriplet();
            System.out.println(hssfColor.getHexString());
            return "FF" + cast.apply(rgb[0]) + cast.apply(rgb[1]) + cast.apply(rgb[2]);
        }

        return null;
    }


    @SneakyThrows
    public static void main(String[] args) {
        final XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream("workplans/СМГ Апрель.xlsx"));

        System.out.println(getFillColorHex(wb.getSheet("Этап III").getRow(139).getCell(0)));
        System.out.println(getFillColorHex(wb.getSheet("Этап III").getRow(141).getCell(0)));
        System.out.println(wb);
    }
}
