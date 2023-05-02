package gazpromcps.smg.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class ExcelService {
    @Getter
    private static final Path statistics = Paths.get("./statistics");

    static {
        try {
            Files.createDirectories(statistics);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RequiredArgsConstructor
    public static class XlsxWriter implements AutoCloseable {
        private final String filename;
        private final XSSFWorkbook workbook = new XSSFWorkbook();
        private XSSFSheet sheet = null;

        public void page(final String pagename) {
            sheet = workbook.getSheet(pagename);
            if (sheet == null) {
                sheet = workbook.createSheet(pagename);
            }
        }

        public void write(final int row, final int col, final String text) {
            if (sheet.getRow(row) == null) {
                sheet.createRow(row);
            }

            XSSFCell cell;
            if ((cell = sheet.getRow(row).getCell(col)) == null) {
                cell = sheet.getRow(row).createCell(col);
            }

            cell.setCellValue(text);
        }

        public void write(final int row, final List<String> content) {
            if (sheet.getRow(row) == null) {
                sheet.createRow(row);
            }
            final XSSFRow r = sheet.getRow(row);

            int index = 0;
            for (final String text : content) {
                XSSFCell cell;
                if ((cell = r.getCell(index)) == null) {
                    cell = r.createCell(index);
                }

                cell.setCellValue(text);
                index++;
            }
        }

        public void color(final int i, final int j, final Color color) {
            if (sheet.getRow(j) == null) {
                sheet.createRow(j);
            }

            XSSFCell cell;
            if ((cell = sheet.getRow(j).getCell(i)) == null) {
                cell = sheet.createRow(j).createCell(i);
            }

            final XSSFCellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFillForegroundColor(new XSSFColor(color, new DefaultIndexedColorMap()));
            cell.setCellStyle(cellStyle);
        }

        public void merge(final int iFrom, final int jFrom, final int iTo, final int jTo) {
            sheet.addMergedRegion(new CellRangeAddress(iFrom, iTo, jFrom, jTo));
        }


        @Override
        public void close() throws Exception {
            final var output = new FileOutputStream(statistics.resolve(filename).toFile());
            workbook.write(output);
            output.close();
            workbook.close();
        }
    }

    public static class XlsxReader implements AutoCloseable {
        public record XlsxPageReader(Sheet sheet) {
            @RequiredArgsConstructor
            public static class XlsxCellReader {
                private static class ColorMapper {
                    private final static Set<String> GREEN = Set.of(
                            "FF22AA44"
                    );
                    private final static Set<String> BLUE = Set.of(
                            "FF33CC33"
                    );

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
                        final org.apache.poi.ss.usermodel.Color color = cellStyle.getFillForegroundColorColor();

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

                    private static boolean isGreen(final Cell cell) {
                        final String fillColorHex = getFillColorHex(cell);
                        return fillColorHex != null && GREEN.contains(fillColorHex);
                    }

                    private static boolean isBlue(final Cell cell) {
                        final String fillColorHex = getFillColorHex(cell);
                        return fillColorHex != null && BLUE.contains(getFillColorHex(cell));
                    }
                }

                private final Row row;

                public boolean isGreen() {
                    return ColorMapper.isGreen(row.getCell(0));
                }

                public boolean isBlue() {
                    return ColorMapper.isBlue(row.getCell(0));
                }

                public String data(final int i) {
                    final var value = row.getCell(i);
                    if (value == null) {
                        return "";
                    } else {
                        return value.toString().strip();
                    }
                }
            }

            public void iterate(final Predicate<XlsxCellReader> startFrom, final Predicate<XlsxCellReader> endAt,
                                       final Consumer<XlsxCellReader> processor) {
                final List<CellRangeAddress> merged = List.of();//sheet.getMergedRegions();

                final Iterable<Row> iterable = sheet::rowIterator;
                StreamSupport.stream(iterable.spliterator(), true)
                        .map(XlsxCellReader::new)
                        .dropWhile(startFrom.negate())
                        .skip(1)
                        .takeWhile(endAt.negate())
                        .filter(row -> (row.row.getRowStyle() == null || !row.row.getRowStyle().getHidden()) &&
                                merged.stream().noneMatch(m -> m.containsRow(row.row.getRowNum())))
                        .forEach(processor);
            }
        }
        private final XSSFWorkbook workbook;

        public XlsxReader(final String filename) throws IOException {
            workbook = new XSSFWorkbook(new FileInputStream(statistics.resolve(filename).toFile()));
        }

        public Stream<XlsxPageReader> pages() {
            final Iterable<Sheet> iterable = workbook::sheetIterator;
            return StreamSupport.stream(iterable.spliterator(), true).parallel().map(XlsxPageReader::new)
                    .peek(s -> log.info("NEW EXCEL - SHEET - " + s.sheet.getSheetName()));
        }

        @Override
        public void close() throws Exception {
            workbook.close();
        }
    }

    public XlsxWriter writeXlsx(final String filename) {
        return new XlsxWriter(filename);
    }

    public XlsxReader readXlsx(final String filename) throws IOException {
        return new XlsxReader(filename);
    }

    public File file(final String filename) {
        return statistics.resolve(filename).toFile();
    }
}
