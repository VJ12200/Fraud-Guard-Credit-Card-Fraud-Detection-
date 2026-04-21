import pandas as pd
import numpy as np
import joblib


def preprocess(df, training=False):
    df = df.copy()

    
    
    
    df['age']  = 2020 - pd.to_datetime(df['dob']).dt.year
    df['hour'] = pd.to_datetime(df['trans_date_trans_time']).dt.hour

    
    
    
    
    df['trans_dt'] = pd.to_datetime(df['trans_date_trans_time'])
    df = df.sort_values(['cc_num', 'trans_dt']).reset_index(drop=True)

    
    
    df['time_since_last_txn'] = (
        df.groupby('cc_num')['trans_dt']
        .diff()
        .dt.total_seconds()
        .fillna(999_999)
    )

    
    df['day'] = df['trans_dt'].dt.date
    df['txn_count_per_card_day'] = (
        df.groupby(['cc_num', 'day'])['amt']
        .transform('count')
        .astype(int)
    )

    
    
    
    
    card_mean = df.groupby('cc_num')['amt'].transform('mean')
    card_std  = df.groupby('cc_num')['amt'].transform('std').fillna(1.0)

    df['amt_vs_card_mean'] = df['amt'] - card_mean          
    df['amt_vs_card_std']  = df['amt'] / (card_std + 1e-6)  

    
    
    
    
    
    df['geo_distance'] = np.sqrt(
        (df['lat'] - df['merch_lat']) ** 2 +
        (df['long'] - df['merch_long']) ** 2
    )

    
    
    
    if training:
        from sklearn.preprocessing import LabelEncoder

        le_category = LabelEncoder()
        le_gender   = LabelEncoder()

        df['category_enc'] = le_category.fit_transform(df['category'])
        df['gender_enc']   = le_gender.fit_transform(df['gender'])

        joblib.dump(le_category, "le_category.pkl")
        joblib.dump(le_gender,   "le_gender.pkl")

    else:
        le_category = joblib.load("le_category.pkl")
        le_gender   = joblib.load("le_gender.pkl")

        
        known_cats = set(le_category.classes_)
        df['category'] = df['category'].apply(
            lambda x: x if x in known_cats else le_category.classes_[0]
        )
        df['category_enc'] = le_category.transform(df['category'])
        df['gender_enc']   = le_gender.transform(df['gender'])

    
    
    
    df = df.drop(columns=['trans_dt', 'day'], errors='ignore')

    return df