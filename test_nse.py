import requests
import json

session = requests.Session()

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
    "Accept-Language": "en-US,en;q=0.9",
    "Connection": "keep-alive",
    "Sec-Ch-Ua": '"Google Chrome";v="125", "Chromium";v="125", "Not.A/Brand";v="24"',
    "Sec-Ch-Ua-Mobile": "?0",
    "Sec-Ch-Ua-Platform": '"Windows"',
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "none",
    "Sec-Fetch-User": "?1",
    "Upgrade-Insecure-Requests": "1"
}

print("1. Visiting Homepage...")
r1 = session.get("https://www.nseindia.com/", headers=headers, timeout=10)
print(f"Homepage status: {r1.status_code}")
print(f"Cookies: {session.cookies.get_dict()}")

api_headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    "Accept": "application/json, text/plain, */*",
    "Accept-Language": "en-US,en;q=0.9",
    "Referer": "https://www.nseindia.com/market-data/live-equity-market",
    "Origin": "https://www.nseindia.com",
    "Connection": "keep-alive",
    "Sec-Ch-Ua": '"Google Chrome";v="125", "Chromium";v="125", "Not.A/Brand";v="24"',
    "Sec-Ch-Ua-Mobile": "?0",
    "Sec-Ch-Ua-Platform": '"Windows"',
    "Sec-Fetch-Dest": "empty",
    "Sec-Fetch-Mode": "cors",
    "Sec-Fetch-Site": "same-origin"
}

print("\n2. Fetching Market Status...")
r_status = session.get("https://www.nseindia.com/api/marketStatus", headers=api_headers, timeout=10)
print(f"Market Status status: {r_status.status_code}")
try:
    print(r_status.json())
except Exception as e:
    print(r_status.text[:200])

print("\n3. Fetching Search Autocomplete for 'RELIANCE'...")
r_search = session.get("https://www.nseindia.com/api/search/autocomplete?q=RELIANCE", headers=api_headers, timeout=10)
print(f"Search status: {r_search.status_code}")
try:
    print(r_search.json())
except Exception as e:
    print(r_search.text[:200])

print("\n4. Fetching Nifty 50 Constituents...")
r2 = session.get("https://www.nseindia.com/api/equity-stockIndices?index=NIFTY%2050", headers=api_headers, timeout=10)
print(f"Constituents status: {r2.status_code}")
try:
    data = r2.json()
    print("Success! Total stocks:", len(data.get("data", [])))
    print("Sample stock:", data.get("data", [])[0] if data.get("data") else "None")
except Exception as e:
    print("Error parsing json:")
    print(r2.text[:500])
