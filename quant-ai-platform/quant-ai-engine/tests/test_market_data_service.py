import unittest

from app.services.market_data_service import MarketDataService


class StubBackendClient:
    def list_market_event_source_configs(self, trace_id=None):
        return [
            {
                'sourceCode': 'NEWS_WIRE',
                'sourceName': 'Mock News Source',
                'sourceCategory': 'NEWS',
                'sourceChannel': 'NEWS_FEED',
                'ingestMode': 'MOCK',
                'enabled': True,
                'endpointUrl': '',
                'upstreamUrl': '',
            },
            {
                'sourceCode': 'CNINFO_PUBLIC_ANNOUNCEMENT',
                'sourceName': 'Cninfo Public Announcement',
                'sourceCategory': 'ANNOUNCEMENT',
                'sourceChannel': 'EXCHANGE_FEED',
                'ingestMode': 'CNINFO_PUBLIC_CRAWLER',
                'enabled': True,
                'endpointUrl': 'https://example.com/cninfo',
                'upstreamUrl': '',
            },
            {
                'sourceCode': 'NEWS_HTTP',
                'sourceName': 'HTTP News Source',
                'sourceCategory': 'NEWS',
                'sourceChannel': 'NEWS_FEED',
                'ingestMode': 'HTTP_JSON',
                'enabled': True,
                'endpointUrl': 'https://example.com/news',
                'upstreamUrl': '',
            },
            {
                'sourceCode': 'NEWS_RSS',
                'sourceName': 'RSS News Source',
                'sourceCategory': 'NEWS',
                'sourceChannel': 'NEWS_FEED',
                'ingestMode': 'RSS_XML',
                'enabled': True,
                'endpointUrl': 'https://example.com/news-rss',
                'upstreamUrl': '',
            },
            {
                'sourceCode': 'POLICY_TRACKER',
                'sourceName': 'Policy Tracker',
                'sourceCategory': 'POLICY',
                'sourceChannel': 'POLICY_MONITOR',
                'ingestMode': 'RSS_XML',
                'enabled': True,
                'endpointUrl': 'https://example.com/policy',
                'upstreamUrl': '',
            },
            {
                'sourceCode': 'RISK_MONITOR',
                'sourceName': 'Risk Monitor',
                'sourceCategory': 'RISK',
                'sourceChannel': 'RISK_MONITOR',
                'ingestMode': 'CSRC_RISK_HTML',
                'enabled': True,
                'endpointUrl': 'https://example.com/csrc-risk',
                'upstreamUrl': '',
            },
        ]

    def preview_market_event_source(
        self,
        source_code,
        target_code,
        target_name=None,
        target_type=None,
        item_count=3,
        trace_id=None,
    ):
        if source_code == 'CNINFO_PUBLIC_ANNOUNCEMENT':
            return {
                'sourceCode': source_code,
                'sourceName': 'Cninfo Public Announcement',
                'items': [
                    {
                        'eventTitle': 'Annual report disclosure',
                        'eventSummary': 'Announcement from exchange channel',
                        'occurredAt': '2026-04-20 09:00:00',
                        'impactLevel': 'HIGH',
                        'sourceUrl': 'https://example.com/a',
                    }
                ],
            }
        if source_code == 'NEWS_HTTP':
            return {
                'sourceCode': source_code,
                'sourceName': 'HTTP News Source',
                'items': [
                    {
                        'eventTitle': 'Company wins new industry orders',
                        'eventSummary': 'HTTP news flash',
                        'occurredAt': '2026-04-20 10:00:00',
                        'impactLevel': 'MEDIUM',
                        'sourceUrl': 'https://example.com/news-1',
                    },
                    {
                        'eventTitle': 'Annual report disclosure',
                        'eventSummary': 'Announcement from exchange channel',
                        'occurredAt': '2026-04-20 09:00:00',
                        'impactLevel': 'HIGH',
                        'sourceUrl': 'https://example.com/a',
                    },
                ],
            }
        if source_code == 'NEWS_RSS':
            return {
                'sourceCode': source_code,
                'sourceName': 'RSS News Source',
                'items': [
                    {
                        'eventTitle': 'Industry media tracks new product progress',
                        'eventSummary': 'RSS news flash',
                        'occurredAt': '2026-04-20 11:00:00',
                        'impactLevel': 'MEDIUM',
                        'sourceUrl': 'https://example.com/news-rss-1',
                    }
                ],
            }
        if source_code == 'POLICY_TRACKER':
            return {
                'sourceCode': source_code,
                'sourceName': 'Policy Tracker',
                'items': [
                    {
                        'eventTitle': 'State Council releases new industry policy',
                        'eventSummary': 'Policy update',
                        'occurredAt': '2026-04-20 12:00:00',
                        'impactLevel': 'HIGH',
                        'sourceUrl': 'https://example.com/policy-1',
                    }
                ],
            }
        if source_code == 'RISK_MONITOR':
            return {
                'sourceCode': source_code,
                'sourceName': 'Risk Monitor',
                'items': [
                    {
                        'eventTitle': 'CSRC publishes administrative penalty decision',
                        'eventSummary': 'Regulatory risk update',
                        'occurredAt': '2026-04-20 13:00:00',
                        'impactLevel': 'HIGH',
                        'sourceUrl': 'https://example.com/csrc-risk-1',
                    }
                ],
            }
        return {'sourceCode': source_code, 'sourceName': source_code, 'items': []}

    def list_market_events(self, target_code, target_name=None, page_size=3, trace_id=None):
        return []

    def list_risk_warnings(self, target_code, target_name=None, page_size=3, trace_id=None):
        return []

    def list_strategy_signals(self, target_code, target_name=None, page_size=3, trace_id=None):
        return []

    def list_market_intelligence(self, target_code, target_name=None, page_size=3, trace_id=None):
        return []

    def get_research_workbench(self, target_code, target_name=None, trace_id=None):
        return {}


class MarketDataServiceTests(unittest.TestCase):
    def test_load_financial_data_aggregates_http_and_rss_real_time_sources(self):
        service = MarketDataService()
        service.backend_client = StubBackendClient()

        result = service.load_financial_data(
            target_code='600519.SH',
            target_name='Kweichow Moutai',
            target_type='STOCK',
            trace_id='trace-market-data',
        )

        self.assertEqual('CNINFO_PUBLIC_ANNOUNCEMENT', result['liveMarketEventSourceCode'])
        self.assertEqual(
            ['CNINFO_PUBLIC_ANNOUNCEMENT', 'NEWS_HTTP', 'NEWS_RSS', 'POLICY_TRACKER', 'RISK_MONITOR'],
            result['liveMarketEventSourceCodes'],
        )
        self.assertEqual(5, len(result['liveMarketEventSources']))
        self.assertEqual(5, result['liveEventCount'])
        self.assertEqual(
            ['CNINFO_PUBLIC_ANNOUNCEMENT', 'NEWS_HTTP', 'NEWS_RSS', 'POLICY_TRACKER', 'RISK_MONITOR'],
            [item['sourceCode'] for item in result['liveMarketEvents']],
        )
        self.assertEqual(1, result['policyLiveEventCount'])
        self.assertEqual(1, result['regulatoryRiskLiveEventCount'])
        self.assertIn('State Council releases new industry policy', result['policyLiveEventHighlights'][0])
        self.assertIn('CSRC publishes administrative penalty decision', result['regulatoryRiskLiveEventHighlights'][0])
        self.assertIn('CSRC publishes administrative penalty decision', result['priorityExternalRiskEventSummary'])


if __name__ == '__main__':
    unittest.main()
