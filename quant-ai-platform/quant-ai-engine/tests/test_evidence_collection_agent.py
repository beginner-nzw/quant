import unittest

from app.agents.evidence_collection_agent import EvidenceCollectionAgent


class EvidenceCollectionAgentTests(unittest.TestCase):
    def setUp(self) -> None:
        self.agent = EvidenceCollectionAgent.__new__(EvidenceCollectionAgent)

    def test_live_event_evidence_splits_policy_and_regulatory_risk(self) -> None:
        market_context = {
            "liveMarketEventSourceCodes": [
                "NEWS_WIRE",
                "POLICY_TRACKER",
                "RISK_MONITOR",
            ],
            "liveMarketEvents": [
                {
                    "eventId": "news-1",
                    "eventTitle": "公司获得重要订单",
                    "eventSummary": "新闻源返回的普通实时事件",
                    "occurredAt": "2026-04-20 14:00:00",
                    "impactLevel": "HIGH",
                    "sourceCode": "NEWS_WIRE",
                    "sourceCategory": "NEWS",
                    "sourceChannel": "NEWS_WIRE",
                },
                {
                    "eventId": "policy-1",
                    "eventTitle": "国务院发布支持先进制造业政策",
                    "eventSummary": "中国政府网政策更新",
                    "occurredAt": "2026-04-20 12:00:00",
                    "impactLevel": "MEDIUM",
                    "eventType": "POLICY",
                    "sourceCode": "POLICY_TRACKER",
                    "sourceCategory": "POLICY",
                    "sourceChannel": "POLICY_MONITOR",
                },
                {
                    "eventId": "risk-1",
                    "eventTitle": "证监会发布行政处罚决定",
                    "eventSummary": "监管风险事件更新",
                    "occurredAt": "2026-04-20 13:00:00",
                    "impactLevel": "LOW",
                    "eventType": "RISK_ALERT",
                    "sourceCode": "RISK_MONITOR",
                    "sourceCategory": "RISK",
                    "sourceChannel": "RISK_MONITOR",
                },
            ],
            "policyLiveEventCount": 1,
            "regulatoryRiskLiveEventCount": 1,
        }

        items = self.agent._build_evidence_items(
            source_context={},
            task_context={},
            source_task_context={},
            market_context=market_context,
        )
        live_items = [
            item
            for item in items
            if item["evidenceType"]
            in {"LIVE_MARKET_EVENT", "POLICY_LIVE_EVENT", "REGULATORY_RISK_LIVE_EVENT"}
        ]

        self.assertEqual(
            [
                "REGULATORY_RISK_LIVE_EVENT",
                "POLICY_LIVE_EVENT",
                "LIVE_MARKET_EVENT",
            ],
            [item["evidenceType"] for item in live_items],
        )
        self.assertEqual("regulatory-risk-live-event:risk-1", live_items[0]["evidenceId"])
        self.assertEqual("policy-live-event:policy-1", live_items[1]["evidenceId"])

        refs = self.agent._build_evidence_refs(items, market_context)

        self.assertIn("regulatoryRiskLiveEvent:risk-1", refs)
        self.assertIn("policyLiveEvent:policy-1", refs)
        self.assertIn("liveMarketEvent:news-1", refs)
        self.assertIn("policyLiveEventCount:1", refs)
        self.assertIn("regulatoryRiskLiveEventCount:1", refs)


if __name__ == "__main__":
    unittest.main()
