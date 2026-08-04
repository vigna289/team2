import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const tradeLatency = new Trend('trade_post_latency_ms');
const tradeErrors = new Rate('trade_post_errors');


export const options = {

    vus: 200,

    duration: '2m',

    thresholds: {

        http_req_failed: [
            'rate<0.02'
        ],

        http_req_duration: [
            'p(95)<800',
            'p(99)<2000'
        ],
trade_post_latency_ms: [
    'p(95)<800',
    'p(99)<2000'
],

trade_post_errors: [
    'rate<0.02'
]

    }

};


export function setup() {

    const response = http.post(

        'http://localhost:8080/auth/login',

        JSON.stringify({

            email: "trader@db.com",

password: "trader123"

        }),

        {

            headers: {

                'Content-Type': 'application/json'

            }

        }

    );


    check(response, {

        'login successful':
            (r) => r.status === 200

    });


    return response.json('token');

}



export default function(token) {


    const trade = JSON.stringify({

        tradeRef: "ABC-20260803-0001",

        instrumentId: 1,

        counterpartyId: 1,

        assetClass: "EQUITY",

        side: "BUY",

        quantity: 10,

        price: 150.50,

        tradeDate: "2026-08-03"

    });



    const response = http.post(

        'http://localhost:8080/api/v1/trades',

        trade,

        {

            headers: {

                Authorization: `Bearer ${token}`,

                'Content-Type': 'application/json'

            }

        }

    );


    check(response, {

        'trade created':
            (r) => r.status === 201

    });


}