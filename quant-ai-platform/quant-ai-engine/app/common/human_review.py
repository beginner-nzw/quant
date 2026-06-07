class HumanReviewRequiredException(Exception):
    def __init__(self, state: dict, node_name: str, reason: str):
        super().__init__(reason)
        self.state = state
        self.node_name = node_name
        self.reason = reason
