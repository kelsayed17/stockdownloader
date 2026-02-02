class FinancialData:
    def __init__(self) -> None:
        self.revenue: list[int] = [0] * 6
        self.basic_shares: list[int] = [0] * 6
        self.diluted_shares: list[int] = [0] * 6
        self.revenue_per_share: list[float] = [0.0] * 6
        self.revenue_per_share_ttm_last_qtr: float = 0.0
        self.fiscal_quarters: list[str] = [""] * 6
        self.incomplete: bool = False
        self.error: bool = False

    def compute_revenue_per_share(self) -> None:
        for i in range(6):
            if self.diluted_shares[i] == 0:
                self.diluted_shares[i] = self.basic_shares[i]
        for i in range(6):
            self.revenue_per_share[i] = (
                self.revenue[i] / self.diluted_shares[i] if self.diluted_shares[i] != 0 else 0.0
            )
        self.revenue_per_share_ttm_last_qtr = round(sum(self.revenue_per_share[:4]), 2)
