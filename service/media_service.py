import os
from zipfile import ZipFile, ZIP_DEFLATED

path_to_media = '/bot/storage/media/'

os.makedirs(path_to_media, exist_ok=True)


class MediaService:
    @staticmethod
    def get_all() -> str:
        snapshot = [path_to_media + f for f in os.listdir(path_to_media)]

        zip_file = path_to_media + 'all_media.zip'

        with ZipFile(zip_file, 'w', ZIP_DEFLATED) as archive:
            for file in snapshot:
                archive.write(file)

        return zip_file

    @staticmethod
    def delete_tmp():
        os.remove(path_to_media + 'all_media.zip')
