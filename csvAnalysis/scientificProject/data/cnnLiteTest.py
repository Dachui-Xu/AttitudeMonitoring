import pandas as pd
import numpy as np
import glob
import tensorflow as tf
from sklearn.preprocessing import StandardScaler
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
    # 加载 TensorFlow Lite 模型
    interpreter = tf.lite.Interpreter(model_path='model.tflite')
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    file_paths = glob.glob('*_noconcat.csv')  # 确保路径正确
    all_predictions = []
    all_labels = []

    for file_path in file_paths:
        df = load_data(file_path)
        features = extract_features_from_data(df, 76)

        scaler = StandardScaler()
        features = scaler.fit_transform(features.reshape(-1, features.shape[-1])).reshape(-1, features.shape[-2],
                                                                                          features.shape[-1])
        features = features.astype(np.float32)  # 确保数据类型为float32以匹配模型输入类型

        # 对每个样本进行预测
        for feature in features:
            interpreter.set_tensor(input_details[0]['index'], [feature])
            interpreter.invoke()
            output_data = interpreter.get_tensor(output_details[0]['index'])
            predicted_label = np.argmax(output_data)
            all_predictions.append(predicted_label)

        # 从文件名中提取状态
        state = file_path.split('/')[-1].split('_')[0]
        all_labels.extend([state] * len(features))

    # 将预测标签索引转换为标签名
    training_labels = ['down', 'left', 'right']
    label_to_index = {label: index for index, label in enumerate(training_labels)}
    index_to_label = {index: label for label, index in label_to_index.items()}
    predicted_labels_names = [index_to_label[label] for label in all_predictions]

    # 统计每种状态的预测次数
    prediction_counts = pd.Series(predicted_labels_names).value_counts().to_dict()
    print("Prediction counts:")
    for state, count in prediction_counts.items():
        print(f"{state}: {count}")

    # 打印总体分类报告
    print("Classification Report:")
    print(
        classification_report(all_labels, predicted_labels_names, labels=training_labels, target_names=training_labels))


main()
