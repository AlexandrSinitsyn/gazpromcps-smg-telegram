class RequestError(Exception):
    bundle_key: str

    def __init__(self, message):
        super().__init__(message)

        self.bundle_key = message
