import pandas as pd
import numpy as np
import glob
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Conv1D, MaxPooling1D, Flatten, Dense, Dropout
from tensorflow.keras.callbacks import EarlyStopping
from tensorflow.keras.utils import to_categorical


def load_data(file_path):
    df = pd.read_csv(file_path)
    return df


def extract_features_from_data(df, window_size=89):
    X = []
    for start in range(0, len(df) - window_size + 1, window_size):
        window = df.iloc[start:start + window_size].values
        X.append(window)
    return np.array(X)


def load_and_process_files(file_paths, window_size=89):
    all_X = []
    all_y = []

    for file_path in file_paths:
        # Extract state from the file name
        state = file_path.split('/')[-1].split('_')[0]

        # Load and process the CSV file
        df = load_data(file_path)

        # Prepare dataset from windows
        X = extract_features_from_data(df, window_size)
        y = [state] * len(X)

        all_X.append(X)
        all_y.extend(y)

    # Combine data from all files
    all_X = np.vstack(all_X)
    all_y = np.array(all_y)

    return all_X, all_y


# 将状态标签转换为数值编码
def encode_labels(labels):
    unique_labels = np.unique(labels)
    label_to_index = {label: index for index, label in enumerate(unique_labels)}
    return np.array([label_to_index[label] for label in labels]), label_to_index


def main():
    # 数据文件路径
    file_paths = glob.glob('*_noconcat.csv')  # Adjust the path as necessary
    window_size = 76  # Set the window size to 89 or 90 based on your preference

    # 加载和处理文件
    all_X, all_y = load_and_process_files(file_paths, window_size)

    # 将状态标签转换为数值编码
    all_y, label_to_index = encode_labels(all_y)
    all_y = to_categorical(all_y)

    # 标准化特征
    scaler = StandardScaler()
    all_X = scaler.fit_transform(all_X.reshape(-1, window_size * 4)).reshape(-1, window_size, 4)

    # 划分训练集和测试集
    X_train, X_test, y_train, y_test = train_test_split(all_X, all_y, test_size=0.2, random_state=42)

    # 创建CNN模型
    model = Sequential()
    model.add(Conv1D(64, kernel_size=3, activation='relu', input_shape=(window_size, 4)))
    model.add(MaxPooling1D(pool_size=2))
    model.add(Conv1D(128, kernel_size=3, activation='relu'))
    model.add(MaxPooling1D(pool_size=2))
    model.add(Flatten())
    model.add(Dense(128, activation='relu'))
    model.add(Dropout(0.5))
    model.add(Dense(len(label_to_index), activation='softmax'))

    model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

    # 训练模型
    early_stopping = EarlyStopping(monitor='val_loss', patience=5, restore_best_weights=True)
    model.fit(X_train, y_train, validation_data=(X_test, y_test), epochs=50, batch_size=32, callbacks=[early_stopping])

    # 评估模型
    loss, accuracy = model.evaluate(X_test, y_test)
    print(f"Test Accuracy: {accuracy}")

    # 保存模型
    model.save('cnn_model.h5')
    print("Model saved as 'cnn_model.h5'")

    # 打印分类报告
    y_pred = model.predict(X_test)
    y_pred_labels = np.argmax(y_pred, axis=1)
    y_test_labels = np.argmax(y_test, axis=1)

    print("Classification Report:")
    print(classification_report(y_test_labels, y_pred_labels, target_names=label_to_index.keys()))


# 运行主函数
main()
