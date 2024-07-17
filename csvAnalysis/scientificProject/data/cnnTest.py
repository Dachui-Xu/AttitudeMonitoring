import pandas as pd
import numpy as np
import glob
from sklearn.preprocessing import StandardScaler
from tensorflow.keras.models import load_model
from sklearn.metrics import classification_report


def load_data(file_path):
    """加载CSV文件数据"""
    return pd.read_csv(file_path)


def extract_features_from_data(df, window_size=76):
    """根据窗口大小准备特征"""
    num_features = df.shape[1]
    num_samples = len(df) // window_size
    X = np.zeros((num_samples, window_size, num_features))
    for i in range(num_samples):
        X[i] = df.iloc[i * window_size:(i + 1) * window_size].values
    return X


def main():
    model = load_model('cnn_model.h5')
    file_paths = glob.glob('sensor_data_20240711_175205.csv')  # 修改为正确的文件路径模式
    all_predictions = []
    all_labels = []

    for file_path in file_paths:
        df = load_data(file_path)
        features = extract_features_from_data(df, 76)

        scaler = StandardScaler()
        features = scaler.fit_transform(features.reshape(-1, features.shape[-1])).reshape(-1, features.shape[-2],
                                                                                          features.shape[-1])
        features = features.astype(np.float32)

        predictions = model.predict(features)
        predicted_labels = np.argmax(predictions, axis=1)

        # 从文件名中提取状态
        state = file_path.split('/')[-1].split('_')[0]
        all_labels.extend([state] * len(features))  # 每个窗口的真实状态

        # 将预测标签转换为状态名称
        training_labels = ['down', 'left', 'right', 'up']  # 确保与训练时的标签一致
        label_to_index = {label: index for index, label in enumerate(training_labels)}
        index_to_label = {index: label for label, index in label_to_index.items()}
        predicted_states = [index_to_label[label] for label in predicted_labels]
        all_predictions.extend(predicted_states)

    # 统计每种状态的预测次数
    prediction_counts = pd.Series(all_predictions).value_counts().to_dict()
    print("Prediction counts:")
    for state, count in prediction_counts.items():
        print(f"{state}: {count}")

    # 打印总体分类报告
    print("Classification Report:")
    print(classification_report(all_labels, all_predictions, labels=training_labels, target_names=training_labels))


main()
