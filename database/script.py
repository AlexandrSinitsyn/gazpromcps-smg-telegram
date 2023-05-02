import logging
import re
import time

import openpyxl

from service.excel_service import *
from colorsys import rgb_to_hls, hls_to_rgb
# From: https://stackoverflow.com/questions/58429823/getting-excel-cell-background-themed-color-as-hex-with-openpyxl/58443509#58443509
#   which refers to: https://pastebin.com/B2nGEGX2 (October 2020)
#       Updated to use list(elem) instead of the deprecated elem.getchildren() method
#       which has now been removed completely from Python 3.9 onwards.
#

#https://bitbucket.org/openpyxl/openpyxl/issues/987/add-utility-functions-for-colors-to-help

RGBMAX = 0xff  # Corresponds to 255
HLSMAX = 240  # MS excel's tint function expects that HLS is base 240. see:
# https://social.msdn.microsoft.com/Forums/en-US/e9d8c136-6d62-4098-9b1b-dac786149f43/excel-color-tint-algorithm-incorrect?forum=os_binaryfile#d3c2ac95-52e0-476b-86f1-e2a697f24969



def rgb_to_ms_hls(red, green=None, blue=None):
    """Converts rgb values in range (0,1) or a hex string of the form '[#aa]rrggbb' to HLSMAX based HLS, (alpha values are ignored)"""
    if green is None:
        if isinstance(red, str):
            if len(red) > 6:
                red = red[-6:]  # Ignore preceding '#' and alpha values
            blue = int(red[4:], 16) / RGBMAX
            green = int(red[2:4], 16) / RGBMAX
            red = int(red[0:2], 16) / RGBMAX
        else:
            red, green, blue = red
    h, l, s = rgb_to_hls(red, green, blue)
    return (int(round(h * HLSMAX)), int(round(l * HLSMAX)), int(round(s * HLSMAX)))


def ms_hls_to_rgb(hue, lightness=None, saturation=None):
    """Converts HLSMAX based HLS values to rgb values in the range (0,1)"""
    if lightness is None:
        hue, lightness, saturation = hue
    return hls_to_rgb(hue / HLSMAX, lightness / HLSMAX, saturation / HLSMAX)


def rgb_to_hex(red, green=None, blue=None):
    """Converts (0,1) based RGB values to a hex string 'rrggbb'"""
    if green is None:
        red, green, blue = red
    return ('FF%02x%02x%02x' % (int(round(red * RGBMAX)), int(round(green * RGBMAX)), int(round(blue * RGBMAX)))).upper()


def get_theme_colors(wb):
    """Gets theme colors from the workbook"""
    # see: https://groups.google.com/forum/#!topic/openpyxl-users/I0k3TfqNLrc
    from openpyxl.xml.functions import QName, fromstring
    xlmns = 'http://schemas.openxmlformats.org/drawingml/2006/main'
    root = fromstring(wb.loaded_theme)
    themeEl = root.find(QName(xlmns, 'themeElements').text)
    colorSchemes = themeEl.findall(QName(xlmns, 'clrScheme').text)
    firstColorScheme = colorSchemes[0]

    colors = []

    for c in ['lt1', 'dk1', 'lt2', 'dk2', 'accent1', 'accent2', 'accent3', 'accent4', 'accent5', 'accent6']:
        accent = firstColorScheme.find(QName(xlmns, c).text)
        for i in list(accent): # walk all child nodes, rather than assuming [0]
            if 'window' in i.attrib['val']:
                colors.append(i.attrib['lastClr'])
            else:
                colors.append(i.attrib['val'])

    return colors


def tint_luminance(tint, lum):
    """Tints a HLSMAX based luminance"""
    # See: http://ciintelligence.blogspot.co.uk/2012/02/converting-excel-theme-color-and-tint.html
    if tint < 0:
        return int(round(lum * (1.0 + tint)))
    else:
        return int(round(lum * (1.0 - tint) + (HLSMAX - HLSMAX * (1.0 - tint))))


def theme_and_tint_to_rgb(wb, theme, tint):
    """Given a workbook, a theme number and a tint return a hex based rgb"""
    rgb = get_theme_colors(wb)[theme]
    h, l, s = rgb_to_ms_hls(rgb)
    return rgb_to_hex(ms_hls_to_rgb(h, tint_luminance(tint, l), s))


BLUE_COLORS = {'FFB8CCE4', 'FFC7DAF1', 'FFB9CDE5',  # found
               # from excel
               'FF00B0F0', 'FF0070C0', 'FF4F80BD', 'FFDBE5F2', 'FF96B3D8', 'FF376093', 'FF254061', 'FF1F497C', 'FF8DB4E3', 'FF558ED5', 'FF17375E', 'FF10253F', 'FF4AADC6', 'FFDBEFF4', 'FFB6DEE8', 'FF93CEDD', 'FF31869B', 'FF215967'}
GREEN_COLORS = {'FF33CC33',  # found
                'FF009900', 'FF49E951', 'FF10DE72', 'FF5AD27F', 'FF00CC00', 'FF66FF33',  # from excel
                'FF92D050', 'FF00B050', 'FF9BBB59', 'FFEBF1DE', 'FFD7E4BD', 'FFC3D69B', 'FF77943C', 'FF4F6228'}


def cell_color(wb, cell):
    if isinstance(cell.fill.fgColor.theme, int):
        return theme_and_tint_to_rgb(wb, cell.fill.start_color.theme, cell.fill.start_color.tint)
    else:
        return cell.fill.fgColor.rgb


def upload(file_name: str):
    # noinspection PyPep8Naming
    START = time.time()
    wb = openpyxl.load_workbook(rf"{file_name}")
    READ = time.time()
    logging.info(f'WORKBOOK READ IN: {READ - START} sec')

    current_master = ''

    result = []

    for sheet_name in wb.sheetnames:
        logging.info(f'WORKSHEET: {sheet_name}')
        current_gen_plan = None

        print()

        sheet = wb[sheet_name]
        print('-' * 10, sheet_name, '-' * 10)
        if sheet.sheet_state == 'visible':
            hidden_rows = {i - 1 for i, r in sheet.row_dimensions.items() if r.hidden}

            wait = True

            for ind, row in enumerate(sheet):
                if ind in hidden_rows or row[1] is None or row[1].value is None:
                    continue

                if row[0].value is not None and row[0].value == '№ п/п':
                    wait = False
                    continue

                if wait:
                    continue

                data = row[1].value
                mst = row[2].value
                measur = row[3].value
                if isinstance(data, str) and any([e in data.strip() for e in \
                                                  ['Технические ресурсы', 'Людские ресурсы',
                                                   'Потребность в людских и технических ресурсах']]):
                    break

                color = cell_color(wb, row[1])
                if color in BLUE_COLORS:
                    if current_gen_plan is None:
                        current_gen_plan = data
                    else:
                        continue

                if color in GREEN_COLORS:
                    current_gen_plan = data

                if mst is not None and mst.strip():
                    m = re.search(r"[^(\n]*(\(([^)]*)\))?(\n(.*))?", mst.strip())
                    current_master = next(item for item in [m.group(i) for i in range(4, -1, -1)] if item is not None)
                    current_master = re.sub(r"\s+", " ", current_master).strip()

                if measur is not None and measur.strip():
                    result.append(Job(-1, sheet_name, current_gen_plan, current_master.strip(), data.strip(),
                                    measur.strip(), True, datetime.now()))

    logging.info(f'WORKBOOK PARSED IN: {time.time() - READ} sec')

    return result
